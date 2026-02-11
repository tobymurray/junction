package com.technicallyrural.junction.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.matrix.MatrixRegistry
import com.technicallyrural.junction.matrix.MatrixSendResult
import com.android.messaging.adapter.SmsStorageAdapter
import com.technicallyrural.junction.persistence.repository.MessageRepository
import com.technicallyrural.junction.persistence.util.AospThreadIdExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives SMS_DELIVER broadcasts when this app is the default SMS app.
 *
 * This receiver handles incoming SMS and:
 * 1. Forwards to AOSP storage via SmsStorageAdapter (always)
 * 2. Forwards to Matrix bridge (if enabled, with persistent deduplication)
 *
 * Architecture:
 * - Replaces AOSP's SmsDeliverReceiver (which is disabled in manifest)
 * - Uses SmsStorageAdapter to maintain AOSP storage/UI functionality
 * - Uses MessageRepository for crash-safe deduplication
 * - Uses core-matrix interfaces for Matrix bridging
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SmsDeliverReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        try {
            Log.d(TAG, "SMS_DELIVER received")

            // CRITICAL: Forward to AOSP storage FIRST to ensure SMS is stored
            // even if Matrix forwarding fails or is disabled
            SmsStorageAdapter.storeSmsFromIntent(context, intent)

            // Extract SMS messages for Matrix forwarding
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages in intent")
                return
            }

            // Process each message for Matrix forwarding
            messages.forEach { smsMessage ->
                val sender = smsMessage.displayOriginatingAddress ?: return@forEach
                val body = smsMessage.messageBody ?: return@forEach
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "SMS from $sender, body length: ${body.length}")

                // Forward to Matrix if enabled
                forwardToMatrix(context, sender, body, timestamp)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS_DELIVER", e)
        }
    }

    /**
     * Forward incoming SMS to Matrix bridge with persistent deduplication.
     */
    private fun forwardToMatrix(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        scope.launch {
            try {
                // Check if Matrix is enabled
                val config = MatrixConfigRepository.getInstance(context).loadConfig()
                if (!config.enabled || !config.isAuthenticated()) {
                    Log.d(TAG, "Matrix not enabled, skipping bridge")
                    return@launch
                }

                // Check if Matrix bridge is initialized
                if (!MatrixRegistry.isInitialized) {
                    Log.d(TAG, "MatrixRegistry not initialized, skipping bridge")
                    return@launch
                }

                // Get conversation ID from AOSP thread system
                val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, sender)

                // Get our phone number (recipient)
                val ownNumber = AospThreadIdExtractor.getOwnPhoneNumber(context) ?: "unknown"

                // Initialize repository
                val messageRepo = MessageRepository.getInstance(context)

                // Record send attempt (with deduplication)
                val record = messageRepo.recordSmsToMatrixSend(
                    conversationId = conversationId,
                    senderAddress = sender,
                    recipientAddresses = listOf(ownNumber),
                    body = body,
                    timestamp = timestamp,
                    isGroup = false,
                    smsMessageId = null
                )

                if (record == null) {
                    Log.w(TAG, "Duplicate SMS detected, skipping Matrix forward")
                    return@launch
                }

                Log.d(TAG, "SMS → Matrix: $sender (dedupKey=${record.dedupKey})")

                // Send to Matrix via registry
                val result = MatrixRegistry.matrixBridge.sendToMatrix(
                    phoneNumber = sender,
                    messageBody = body,
                    timestamp = timestamp,
                    isGroup = false
                )

                when (result) {
                    is MatrixSendResult.Success -> {
                        Log.d(TAG, "SMS forwarded to Matrix: eventId=${result.eventId}")
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
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding SMS to Matrix", e)
            }
        }
    }
}
