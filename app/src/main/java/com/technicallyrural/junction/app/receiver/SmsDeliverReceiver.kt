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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Collections

/**
 * Receives SMS_DELIVER broadcasts when this app is the default SMS app.
 *
 * This receiver handles incoming SMS and:
 * 1. Forwards to AOSP storage via SmsStorageAdapter (always)
 * 2. Forwards to Matrix bridge (if enabled)
 *
 * Architecture:
 * - Replaces AOSP's SmsDeliverReceiver (which is disabled in manifest)
 * - Uses SmsStorageAdapter to maintain AOSP storage/UI functionality
 * - Uses core-matrix interfaces for Matrix bridging
 * - Normal SMS functionality works independently of Matrix
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SmsDeliverReceiver"
        private const val MAX_CACHE_SIZE = 1000

        /**
         * Deduplication cache for SMS → Matrix forwarding.
         * Stores message IDs (hash of timestamp + address + body) to prevent
         * duplicate forwarding on app crash/restart or system redelivery.
         *
         * Uses synchronized LRU map with automatic eviction after 1000 entries.
         * Thread-safe for concurrent access from multiple BroadcastReceiver instances.
         */
        private val forwardedMessageIds: MutableSet<String> = Collections.newSetFromMap(
            Collections.synchronizedMap(
                object : LinkedHashMap<String, Boolean>(MAX_CACHE_SIZE + 1, 0.75f, true) {
                    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                        return size > MAX_CACHE_SIZE
                    }
                }
            )
        )

        /**
         * Generate unique message ID for deduplication.
         * Uses SHA-256 hash of (timestamp + address + body) to create
         * deterministic ID that survives app restarts for same message.
         */
        private fun generateMessageId(timestamp: Long, address: String, body: String): String {
            val input = "$timestamp:$address:$body"
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }.substring(0, 16)
        }
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

                // Generate message ID for deduplication
                val messageId = generateMessageId(timestamp, sender, body)

                // Check deduplication cache
                if (forwardedMessageIds.contains(messageId)) {
                    Log.w(TAG, "Duplicate SMS detected (id=$messageId), skipping Matrix forward")
                    return@forEach
                }

                Log.d(TAG, "SMS from $sender, body length: ${body.length}, id=$messageId")

                // Add to cache before forwarding to prevent race conditions
                forwardedMessageIds.add(messageId)

                // Forward to Matrix if enabled
                forwardToMatrix(context, sender, body, timestamp, messageId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS_DELIVER", e)
        }
    }

    /**
     * Forward incoming SMS to Matrix bridge.
     *
     * @param messageId Unique message ID for logging/tracking
     */
    private fun forwardToMatrix(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long,
        messageId: String
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

                Log.d(TAG, "SMS → Matrix: $sender (id=$messageId)")

                // Send to Matrix via registry
                val result = MatrixRegistry.matrixBridge.sendToMatrix(
                    phoneNumber = sender,
                    messageBody = body,
                    timestamp = timestamp,
                    isGroup = false
                )

                when (result) {
                    is MatrixSendResult.Success -> {
                        Log.d(TAG, "SMS forwarded to Matrix: eventId=${result.eventId}, messageId=$messageId")
                    }
                    is MatrixSendResult.Failure -> {
                        Log.e(TAG, "Matrix send failed for messageId=$messageId: ${result.error}")
                        // Note: We keep messageId in cache even on failure to prevent
                        // infinite retry loops. Retry logic should be handled by
                        // WorkManager (Phase 1 work).
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding SMS to Matrix (messageId=$messageId)", e)
                // Keep messageId in cache even on exception to prevent retry storms
            }
        }
    }
}
