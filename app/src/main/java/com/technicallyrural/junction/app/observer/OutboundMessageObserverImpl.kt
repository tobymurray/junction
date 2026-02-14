package com.technicallyrural.junction.app.observer

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.*
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.app.worker.RetryPendingMatrixMessagesWorker
import com.technicallyrural.junction.core.transport.OutboundMessageObserver
import com.technicallyrural.junction.matrix.MatrixRegistry
import com.technicallyrural.junction.matrix.MatrixSendResult
import com.technicallyrural.junction.persistence.repository.MessageRepository
import com.technicallyrural.junction.persistence.util.AospThreadIdExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Implementation of OutboundMessageObserver for Matrix bridging.
 *
 * Called by AOSP's SendStatusReceiver after message send completes.
 * Bridges successfully sent messages to Matrix using MessageRepository.
 *
 * Architecture:
 * - Implements core-sms OutboundMessageObserver interface
 * - Registered in CoreSmsRegistry during app startup
 * - Called by sms-upstream SendStatusReceiver (observer pattern)
 * - No direct AOSP coupling (uses public ContentProvider API)
 *
 * Reliability guarantees:
 * - Only bridges messages with RESULT_OK (carrier confirmed)
 * - Deduplication via MessageRepository (smsMessageId + dedupKey)
 * - WorkManager retry on failure (exponential backoff)
 * - Crash-safe (asynchronous with persistent state)
 */
class OutboundMessageObserverImpl(
    private val context: Context
) : OutboundMessageObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "OutboundObserver"
    }

    override fun onMessageSent(messageUri: Uri, resultCode: Int, isSms: Boolean) {
        Log.d(TAG, "onMessageSent: uri=$messageUri, result=$resultCode, isSms=$isSms")

        // Only bridge successfully sent messages
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Message send failed (resultCode=$resultCode), not bridging to Matrix")
            return
        }

        // Extract message ID from URI (content://sms/<id> or content://mms/<id>)
        val messageId = messageUri.lastPathSegment?.toLongOrNull()
        if (messageId == null) {
            Log.e(TAG, "Invalid message URI: $messageUri")
            return
        }

        Log.d(TAG, "Message sent successfully: id=$messageId, isSms=$isSms")

        if (isSms) {
            // Bridge SMS to Matrix asynchronously
            scope.launch {
                bridgeOutboundSmsToMatrix(messageId)
            }
        } else {
            // TODO: Bridge MMS to Matrix (Phase 1)
            Log.w(TAG, "MMS → Matrix bridging not yet implemented")
        }
    }

    override fun onDeliveryReportReceived(messageUri: Uri, status: Int) {
        // Delivery reports are optional; we bridge on SENT not DELIVERED
        // This avoids waiting for carrier delivery confirmation which may
        // be delayed or never arrive
        Log.d(TAG, "Delivery report received: uri=$messageUri, status=$status (no action needed)")
    }

    /**
     * Bridge outbound SMS to Matrix with crash-safe deduplication.
     */
    private suspend fun bridgeOutboundSmsToMatrix(smsMessageId: Long) {
        try {
            // Check if Matrix is enabled
            val config = MatrixConfigRepository.getInstance(context).loadConfig()
            if (!config.enabled || !config.isAuthenticated()) {
                Log.d(TAG, "Matrix not enabled, skipping outbound bridge")
                return
            }

            // Check if Matrix bridge is initialized
            if (!MatrixRegistry.isInitialized) {
                Log.d(TAG, "MatrixRegistry not initialized, skipping outbound bridge")
                return
            }

            // Read message from AOSP database
            val message = readSmsFromAospDb(smsMessageId)
            if (message == null) {
                Log.e(TAG, "Failed to read SMS message $smsMessageId from AOSP database")
                return
            }

            Log.d(TAG, "Bridging outbound SMS: id=$smsMessageId, to=${message.address}, body=${message.body.take(50)}...")

            // Initialize repository
            val messageRepo = MessageRepository.getInstance(context)

            // Record send attempt (with deduplication)
            val record = messageRepo.recordPhoneToMatrixSend(
                smsMessageId = smsMessageId,
                conversationId = message.threadId.toString(),
                senderAddress = message.selfPhone,
                recipientAddresses = listOf(message.address),
                body = message.body,
                timestamp = message.timestamp,
                isGroup = false
            )

            if (record == null) {
                Log.w(TAG, "Duplicate outbound SMS detected, skipping Matrix bridge: smsMessageId=$smsMessageId")
                return
            }

            Log.d(TAG, "Phone → Matrix: ${message.address} (dedupKey=${record.dedupKey})")

            // Send to Matrix via registry
            val result = MatrixRegistry.matrixBridge.sendToMatrix(
                phoneNumber = message.address,
                messageBody = message.body,
                timestamp = message.timestamp,
                isGroup = false
            )

            when (result) {
                is MatrixSendResult.Success -> {
                    Log.d(TAG, "Outbound SMS bridged to Matrix: eventId=${result.eventId}")
                    // Confirm send
                    messageRepo.confirmMatrixSend(
                        dedupKey = record.dedupKey,
                        matrixEventId = result.eventId,
                        matrixRoomId = result.roomId ?: ""
                    )
                }
                is MatrixSendResult.Failure -> {
                    Log.e(TAG, "Matrix send failed: ${result.error}")
                    // Record failure
                    messageRepo.recordMatrixSendFailure(
                        dedupKey = record.dedupKey,
                        failureReason = result.error.name
                    )
                    // Schedule WorkManager retry
                    scheduleRetryWorker()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error bridging outbound SMS to Matrix", e)
        }
    }

    /**
     * Read SMS message from AOSP telephony database.
     */
    private fun readSmsFromAospDb(smsMessageId: Long): SmsMessage? {
        val uri = Uri.parse("content://sms/$smsMessageId")
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: return null
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                // Only bridge outbound messages (type = SENT)
                if (type != Telephony.Sms.MESSAGE_TYPE_SENT) {
                    Log.w(TAG, "Message $smsMessageId is not outbound (type=$type), skipping")
                    return null
                }

                // Get our phone number
                val selfPhone = AospThreadIdExtractor.getOwnPhoneNumber(context) ?: "unknown"

                return SmsMessage(
                    id = id,
                    threadId = threadId,
                    address = address,
                    body = body,
                    timestamp = timestamp,
                    selfPhone = selfPhone
                )
            }
        }

        return null
    }

    /**
     * Schedule WorkManager retry job for failed Matrix sends.
     */
    private fun scheduleRetryWorker() {
        val workRequest = OneTimeWorkRequestBuilder<RetryPendingMatrixMessagesWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "retry_matrix_bridge",
            ExistingWorkPolicy.KEEP, // Don't restart if already scheduled
            workRequest
        )

        Log.d(TAG, "Scheduled WorkManager retry for failed Matrix sends")
    }

    /**
     * Data class for SMS message from AOSP database.
     */
    private data class SmsMessage(
        val id: Long,
        val threadId: Long,
        val address: String,
        val body: String,
        val timestamp: Long,
        val selfPhone: String
    )
}
