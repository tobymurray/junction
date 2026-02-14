package com.technicallyrural.junction.app.receiver

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
 * Boot receiver for recovering unbridged messages after crashes or reboots.
 *
 * This receiver:
 * 1. Fires on BOOT_COMPLETED
 * 2. Scans AOSP database for sent messages in last 24 hours
 * 3. Checks if each message has been bridged to Matrix
 * 4. Bridges any unbridged messages (crash recovery)
 * 5. Schedules WorkManager retry for pending messages
 *
 * Failure scenarios handled:
 * - App killed before bridging outbound SMS
 * - App killed during bridging (pending messages)
 * - Device rebooted during send
 * - Matrix server down during original send
 *
 * Reliability guarantees:
 * - Idempotent (safe to run multiple times)
 * - Deduplication prevents duplicate Matrix sends
 * - Only scans recent messages (24h window)
 * - Respects Matrix disabled/unauthenticated state
 */
class MatrixBridgeBootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MatrixBootReceiver"
        private const val SCAN_WINDOW_HOURS = 24L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "Boot completed, scanning for unbridged messages")

        // Scan for unbridged messages asynchronously
        scope.launch {
            scanAndBridgeUnbridgedMessages(context)
        }
    }

    /**
     * Scan AOSP database for unbridged messages and bridge them.
     *
     * This ensures no messages are lost due to crashes.
     */
    private suspend fun scanAndBridgeUnbridgedMessages(context: Context) {
        try {
            // Check if Matrix is enabled
            val config = MatrixConfigRepository.getInstance(context).loadConfig()
            if (!config.enabled || !config.isAuthenticated()) {
                Log.d(TAG, "Matrix not enabled, skipping boot scan")
                return
            }

            // Check if Matrix bridge is initialized
            if (!MatrixRegistry.isInitialized) {
                Log.d(TAG, "MatrixRegistry not initialized, skipping boot scan")
                // Schedule retry for later
                scheduleRetryWorker(context)
                return
            }

            val messageRepo = MessageRepository.getInstance(context)

            // Scan for unbridged messages in last 24 hours
            val unbridgedMessages = scanAospDatabaseForUnbridged(context, messageRepo)

            if (unbridgedMessages.isEmpty()) {
                Log.d(TAG, "No unbridged messages found")
            } else {
                Log.d(TAG, "Found ${unbridgedMessages.size} unbridged messages, bridging...")

                unbridgedMessages.forEach { sms ->
                    bridgeUnbridgedMessage(context, messageRepo, sms)
                }
            }

            // Schedule WorkManager to retry any pending messages
            scheduleRetryWorker(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for unbridged messages", e)
        }
    }

    /**
     * Scan AOSP SMS database for sent messages without bridge records.
     */
    private suspend fun scanAospDatabaseForUnbridged(
        context: Context,
        messageRepo: MessageRepository
    ): List<UnbridgedSms> {
        val cutoffTime = System.currentTimeMillis() - (SCAN_WINDOW_HOURS * 60 * 60 * 1000)
        val unbridged = mutableListOf<UnbridgedSms>()

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val selection = "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(
            Telephony.Sms.MESSAGE_TYPE_SENT.toString(),
            cutoffTime.toString()
        )
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: continue
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))

                // Check if already bridged
                if (!messageRepo.existsBySmsMessageId(id)) {
                    unbridged.add(UnbridgedSms(id, threadId, address, body, timestamp))
                }
            }
        }

        return unbridged
    }

    /**
     * Bridge a single unbridged message to Matrix.
     */
    private suspend fun bridgeUnbridgedMessage(
        context: Context,
        messageRepo: MessageRepository,
        sms: UnbridgedSms
    ) {
        try {
            Log.d(TAG, "Bridging unbridged message: id=${sms.id}, to=${sms.address}")

            // Get our phone number
            val selfPhone = AospThreadIdExtractor.getOwnPhoneNumber(context) ?: "unknown"

            // Record send attempt (with deduplication)
            val record = messageRepo.recordPhoneToMatrixSend(
                smsMessageId = sms.id,
                conversationId = sms.threadId.toString(),
                senderAddress = selfPhone,
                recipientAddresses = listOf(sms.address),
                body = sms.body,
                timestamp = sms.timestamp,
                isGroup = false
            )

            if (record == null) {
                Log.w(TAG, "Message ${sms.id} already bridged (dedup)")
                return
            }

            // Send to Matrix
            val result = MatrixRegistry.matrixBridge.sendToMatrix(
                phoneNumber = sms.address,
                messageBody = sms.body,
                timestamp = sms.timestamp,
                isGroup = false
            )

            when (result) {
                is MatrixSendResult.Success -> {
                    Log.d(TAG, "Unbridged message ${sms.id} bridged successfully: eventId=${result.eventId}")
                    messageRepo.confirmMatrixSend(
                        dedupKey = record.dedupKey,
                        matrixEventId = result.eventId,
                        matrixRoomId = result.roomId ?: ""
                    )
                }
                is MatrixSendResult.Failure -> {
                    Log.e(TAG, "Failed to bridge unbridged message ${sms.id}: ${result.error}")
                    messageRepo.recordMatrixSendFailure(
                        dedupKey = record.dedupKey,
                        failureReason = result.error.name
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error bridging unbridged message ${sms.id}", e)
        }
    }

    /**
     * Schedule WorkManager retry for pending messages.
     */
    private fun scheduleRetryWorker(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<RetryPendingMatrixMessagesWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "retry_matrix_bridge",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        Log.d(TAG, "Scheduled WorkManager retry for pending messages")
    }

    /**
     * Data class for unbridged SMS from AOSP database.
     */
    private data class UnbridgedSms(
        val id: Long,
        val threadId: Long,
        val address: String,
        val body: String,
        val timestamp: Long
    )
}
