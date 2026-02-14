package com.technicallyrural.junction.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.matrix.MatrixRegistry
import com.technicallyrural.junction.matrix.MatrixSendResult
import com.technicallyrural.junction.persistence.model.Direction
import com.technicallyrural.junction.persistence.repository.MessageRepository

/**
 * WorkManager worker for retrying failed Matrix sends.
 *
 * This worker:
 * 1. Queries all pending messages (PENDING status, retry count < MAX_RETRIES)
 * 2. Attempts to send each to Matrix
 * 3. Updates status based on result (CONFIRMED or increments retry count)
 * 4. Automatically reschedules on failure via WorkManager backoff policy
 *
 * Scheduled by:
 * - SmsSentStatusReceiver when outbound SMS → Matrix send fails
 * - MatrixSyncService when Matrix → SMS send fails
 * - MatrixBridgeBootReceiver after scanning for unbridged messages
 *
 * Reliability guarantees:
 * - Idempotent (safe to run multiple times)
 * - Crash-safe (queries database for pending state)
 * - Exponential backoff (via WorkManager configuration)
 * - Max retry limit prevents infinite loops
 */
class RetryPendingMatrixMessagesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RetryMatrixWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting retry of pending Matrix messages")

        try {
            // Check if Matrix is enabled
            val config = MatrixConfigRepository.getInstance(applicationContext).loadConfig()
            if (!config.enabled || !config.isAuthenticated()) {
                Log.d(TAG, "Matrix not enabled, skipping retry")
                return Result.success()
            }

            // Check if Matrix bridge is initialized
            if (!MatrixRegistry.isInitialized) {
                Log.d(TAG, "MatrixRegistry not initialized, skipping retry")
                return Result.retry()
            }

            val messageRepo = MessageRepository.getInstance(applicationContext)

            // Get all pending outbound messages (SMS → Matrix)
            val pendingOutbound = messageRepo.getPendingMessages(Direction.SMS_TO_MATRIX)
            Log.d(TAG, "Found ${pendingOutbound.size} pending outbound messages")

            var successCount = 0
            var failureCount = 0

            pendingOutbound.forEach { message ->
                try {
                    Log.d(TAG, "Retrying outbound message: dedupKey=${message.dedupKey}")

                    // Get recipient from participants
                    val participants = messageRepo.getParticipants(message.id)
                    val recipient = participants.firstOrNull { it.participantType == com.technicallyrural.junction.persistence.entity.ParticipantType.RECIPIENT }

                    if (recipient == null) {
                        Log.e(TAG, "No recipient found for message ${message.id}")
                        failureCount++
                        return@forEach
                    }

                    // Reconstruct message body from hash (not stored in entity)
                    // For retry, we need to re-read from AOSP database
                    val smsMessage = message.smsMessageId?.let { readSmsBody(it) }
                    if (smsMessage == null) {
                        Log.e(TAG, "Cannot read SMS body for retry: smsMessageId=${message.smsMessageId}")
                        messageRepo.recordMatrixSendFailure(
                            dedupKey = message.dedupKey,
                            failureReason = "SMS_NOT_FOUND"
                        )
                        failureCount++
                        return@forEach
                    }

                    // Send to Matrix via registry
                    val result = MatrixRegistry.matrixBridge.sendToMatrix(
                        phoneNumber = recipient.phoneNumber,
                        messageBody = smsMessage,
                        timestamp = message.timestamp,
                        isGroup = message.isGroup
                    )

                    when (result) {
                        is MatrixSendResult.Success -> {
                            Log.d(TAG, "Retry succeeded: eventId=${result.eventId}")
                            messageRepo.confirmMatrixSend(
                                dedupKey = message.dedupKey,
                                matrixEventId = result.eventId,
                                matrixRoomId = result.roomId ?: ""
                            )
                            successCount++
                        }
                        is MatrixSendResult.Failure -> {
                            Log.e(TAG, "Retry failed: ${result.error}")
                            messageRepo.recordMatrixSendFailure(
                                dedupKey = message.dedupKey,
                                failureReason = result.error.name
                            )
                            failureCount++
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error retrying message ${message.dedupKey}", e)
                    failureCount++
                }
            }

            // Get all pending inbound messages (Matrix → SMS)
            val pendingInbound = messageRepo.getPendingMessages(Direction.MATRIX_TO_SMS)
            Log.d(TAG, "Found ${pendingInbound.size} pending inbound messages")

            // TODO: Implement Matrix → SMS retry when that direction is implemented

            Log.d(TAG, "Retry complete: success=$successCount, failed=$failureCount")

            return if (failureCount > 0) {
                // Some messages failed, schedule another retry
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in retry worker", e)
            return Result.retry()
        }
    }

    /**
     * Read SMS message body from AOSP database.
     *
     * Used during retry to reconstruct message content.
     */
    private fun readSmsBody(smsMessageId: Long): String? {
        val uri = android.net.Uri.parse("content://sms/$smsMessageId")
        val projection = arrayOf(android.provider.Telephony.Sms.BODY)

        applicationContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(android.provider.Telephony.Sms.BODY))
            }
        }

        return null
    }
}
