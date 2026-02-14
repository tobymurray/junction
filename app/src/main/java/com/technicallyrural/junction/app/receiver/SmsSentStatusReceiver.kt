package com.technicallyrural.junction.app.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.*
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.app.worker.RetryPendingMatrixMessagesWorker
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
 * Receives MESSAGE_SENT_ACTION broadcasts when SMS/MMS is sent via carrier.
 *
 * This receiver handles outbound message bridging:
 * 1. Waits for carrier send confirmation (RESULT_OK)
 * 2. Extracts message details from AOSP database
 * 3. Bridges to Matrix (if enabled, with persistent deduplication)
 * 4. Schedules WorkManager retry on failure
 *
 * Architecture:
 * - Runs in parallel with AOSP's SendStatusReceiver (both fire for same broadcast)
 * - AOSP receiver handles telephony database updates
 * - This receiver handles Matrix bridging
 * - Uses MessageRepository for crash-safe deduplication
 * - Uses core-matrix interfaces for Matrix bridging
 *
 * Reliability guarantees:
 * - Only bridges messages that successfully sent via carrier
 * - Deduplication prevents duplicate Matrix sends on retry/reboot
 * - WorkManager ensures eventual consistency on failure
 * - Atomic database transactions prevent partial state
 */
class SmsSentStatusReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SmsSentStatusReceiver"

        // AOSP's custom actions (defined in sms-upstream SendStatusReceiver)
        private const val MESSAGE_SENT_ACTION =
            "com.android.messaging.receiver.SendStatusReceiver.MESSAGE_SENT"
        private const val MESSAGE_DELIVERED_ACTION =
            "com.android.messaging.receiver.SendStatusReceiver.MESSAGE_DELIVERED"
        private const val MMS_SENT_ACTION =
            "com.android.messaging.receiver.SendStatusReceiver.MMS_SENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            MESSAGE_SENT_ACTION -> handleSmsSent(context, intent)
            MMS_SENT_ACTION -> handleMmsSent(context, intent)
            MESSAGE_DELIVERED_ACTION -> {
                // Delivery reports are optional; we bridge on SENT not DELIVERED
                // This avoids waiting for carrier delivery confirmation which may
                // be delayed or never arrive
                Log.d(TAG, "Delivery report received (no action needed)")
            }
        }
    }

    /**
     * Handle SMS sent callback.
     *
     * Invoked when SmsManager.sendTextMessage() completes.
     * Intent data URI contains the SMS message URI (content://sms/<id>).
     */
    private fun handleSmsSent(context: Context, intent: Intent) {
        val resultCode = resultCode
        val messageUri = intent.data

        Log.d(TAG, "SMS sent callback: resultCode=$resultCode, uri=$messageUri")

        // Only bridge successfully sent messages
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "SMS send failed (resultCode=$resultCode), not bridging to Matrix")
            return
        }

        if (messageUri == null) {
            Log.e(TAG, "SMS sent callback missing message URI")
            return
        }

        // Extract SMS message ID from URI (content://sms/12345 → 12345)
        val smsMessageId = messageUri.lastPathSegment?.toLongOrNull()
        if (smsMessageId == null) {
            Log.e(TAG, "Invalid SMS message URI: $messageUri")
            return
        }

        Log.d(TAG, "SMS sent successfully: messageId=$smsMessageId")

        // Bridge to Matrix asynchronously
        scope.launch {
            bridgeOutboundSmsToMatrix(context, smsMessageId)
        }
    }

    /**
     * Handle MMS sent callback.
     *
     * Invoked when MMS send completes (success or failure).
     * Intent extras contain message ID and send status.
     */
    private fun handleMmsSent(context: Context, intent: Intent) {
        val resultCode = resultCode

        Log.d(TAG, "MMS sent callback: resultCode=$resultCode")

        // Only bridge successfully sent messages
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "MMS send failed (resultCode=$resultCode), not bridging to Matrix")
            return
        }

        // TODO: MMS handling implementation
        // Extract MMS message ID, media parts, and bridge to Matrix with media upload
        Log.w(TAG, "MMS → Matrix bridging not yet implemented")
    }

    /**
     * Bridge outbound SMS to Matrix with crash-safe deduplication.
     *
     * Flow:
     * 1. Read message from AOSP database
     * 2. Check if already bridged (deduplication)
     * 3. Record bridge attempt in persistence layer
     * 4. Send to Matrix
     * 5. Confirm send or record failure
     * 6. Schedule retry on failure
     */
    private suspend fun bridgeOutboundSmsToMatrix(
        context: Context,
        smsMessageId: Long
    ) {
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
            val message = readSmsFromAospDb(context, smsMessageId)
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
                    scheduleRetryWorker(context)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error bridging outbound SMS to Matrix", e)
        }
    }

    /**
     * Read SMS message from AOSP telephony database.
     *
     * Queries content://sms for message details.
     */
    private fun readSmsFromAospDb(context: Context, smsMessageId: Long): SmsMessage? {
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
     *
     * Uses exponential backoff to avoid overwhelming Matrix server.
     */
    private fun scheduleRetryWorker(context: Context) {
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
