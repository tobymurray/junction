package com.technicallyrural.junction.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.matrix.MatrixRegistry
import com.technicallyrural.junction.matrix.MatrixSendResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives SMS_DELIVER broadcasts when this app is the default SMS app.
 *
 * This receiver handles incoming SMS and:
 * 1. Forwards to Matrix bridge (if enabled)
 * 2. Lets upstream SMS processing handle storage/notifications
 *
 * Architecture:
 * - Uses core-sms interfaces only (no upstream internals)
 * - Matrix bridging is optional based on config
 * - Normal SMS functionality works independently
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        try {
            // Extract SMS messages from intent
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages in intent")
                return
            }

            // Process each message
            messages.forEach { smsMessage ->
                val sender = smsMessage.displayOriginatingAddress ?: return@forEach
                val body = smsMessage.messageBody ?: return@forEach
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "Received SMS from $sender: $body")

                // Forward to Matrix if enabled
                forwardToMatrix(context, sender, body, timestamp)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS_DELIVER", e)
        }

        // Let upstream SMS processing continue
        // (storage, notifications, etc. handled by AOSP code)
    }

    /**
     * Forward incoming SMS to Matrix bridge.
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

                Log.d(TAG, "SMS → Matrix: $sender → $body")

                // Send to Matrix via registry
                val result = MatrixRegistry.matrixBridge.sendToMatrix(
                    phoneNumber = sender,
                    messageBody = body,
                    timestamp = timestamp,
                    isGroup = false
                )

                when (result) {
                    is MatrixSendResult.Success -> {
                        Log.d(TAG, "SMS forwarded to Matrix: ${result.eventId}")
                    }
                    is MatrixSendResult.Failure -> {
                        Log.e(TAG, "Matrix send failed: ${result.error}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding SMS to Matrix", e)
            }
        }
    }

    companion object {
        private const val TAG = "SmsDeliverReceiver"
    }
}
