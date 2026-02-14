package com.technicallyrural.junction.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Receives SMS_DELIVER broadcasts when this app is the default SMS app.
 *
 * This receiver handles incoming SMS and:
 * 1. Forwards to AOSP storage via SmsStorageAdapter (always)
 * 2. Waits for AOSP to reassemble multi-part SMS
 * 3. Bridges complete message to Matrix (if enabled, with persistent deduplication)
 *
 * Multi-part SMS handling:
 * - AOSP automatically reassembles multi-part SMS and stores complete message
 * - We delay bridging to allow reassembly to complete (500ms)
 * - Bridge complete message from database (ensures correct order + single Matrix message)
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

        // Delay to allow AOSP to reassemble multi-part SMS before bridging
        // Multi-part SMS parts can arrive out of order and need time to reassemble
        private const val REASSEMBLY_DELAY_MS = 500L
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

            // Extract SMS messages to identify sender and timestamp
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages in intent")
                return
            }

            // Get sender and timestamp from first message part
            val firstMessage = messages[0]
            val sender = firstMessage.displayOriginatingAddress
            val timestamp = firstMessage.timestampMillis

            if (sender == null) {
                Log.w(TAG, "SMS has no sender address")
                return
            }

            Log.d(TAG, "SMS from $sender with ${messages.size} part(s)")

            // Bridge to Matrix after delay (allows AOSP to reassemble multi-part SMS)
            scope.launch {
                try {
                    // Wait for AOSP to reassemble multi-part messages
                    delay(REASSEMBLY_DELAY_MS)

                    // Bridge complete message from database
                    bridgeCompleteMessageFromDatabase(context, sender, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error bridging SMS to Matrix", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS_DELIVER", e)
        }
    }

    /**
     * Bridge complete SMS message from AOSP database to Matrix.
     *
     * This approach ensures:
     * - Multi-part SMS is fully reassembled by AOSP before bridging
     * - Messages appear in Matrix in correct order
     * - Single Matrix message instead of multiple parts
     *
     * @param sender Phone number of sender
     * @param timestamp Timestamp from first message part (used to find message in DB)
     */
    private suspend fun bridgeCompleteMessageFromDatabase(
        context: Context,
        sender: String,
        timestamp: Long
    ) {
        // Check if Matrix is enabled
        val config = MatrixConfigRepository.getInstance(context).loadConfig()
        if (!config.enabled || !config.isAuthenticated()) {
            Log.d(TAG, "Matrix not enabled, skipping bridge")
            return
        }

        // Check if Matrix bridge is initialized
        if (!MatrixRegistry.isInitialized) {
            Log.d(TAG, "MatrixRegistry not initialized, skipping bridge")
            return
        }

        // Query AOSP database for the complete, reassembled message
        val smsMessage = findSmsInDatabase(context, sender, timestamp)
        if (smsMessage == null) {
            Log.e(TAG, "Failed to find SMS in database for sender=$sender, timestamp=$timestamp")
            return
        }

        Log.d(TAG, "Found complete SMS in database: id=${smsMessage.id}, body length=${smsMessage.body.length}")

        // Get conversation ID from AOSP thread system
        val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, sender)

        // Get our phone number (recipient)
        val ownNumber = AospThreadIdExtractor.getOwnPhoneNumber(context) ?: "unknown"

        // Initialize repository
        val messageRepo = MessageRepository.getInstance(context)

        // Record send attempt (with deduplication)
        // Use SMS message ID for deduplication to prevent duplicate bridging
        val record = messageRepo.recordSmsToMatrixSend(
            conversationId = conversationId,
            senderAddress = sender,
            recipientAddresses = listOf(ownNumber),
            body = smsMessage.body,
            timestamp = smsMessage.timestamp,
            isGroup = false,
            smsMessageId = smsMessage.id
        )

        if (record == null) {
            Log.w(TAG, "Duplicate SMS detected, skipping Matrix forward (id=${smsMessage.id})")
            return
        }

        Log.d(TAG, "SMS → Matrix: $sender (dedupKey=${record.dedupKey}, smsId=${smsMessage.id})")

        // Send to Matrix via registry
        val result = MatrixRegistry.matrixBridge.sendToMatrix(
            phoneNumber = sender,
            messageBody = smsMessage.body,
            timestamp = smsMessage.timestamp,
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
    }

    /**
     * Find SMS message in AOSP database by sender and timestamp.
     *
     * Queries the telephony database for the most recent message from the sender
     * around the given timestamp (±10 seconds window to handle clock skew).
     *
     * This gives us the complete, reassembled message that AOSP has stored.
     */
    private fun findSmsInDatabase(
        context: Context,
        sender: String,
        timestamp: Long
    ): SmsMessage? {
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        // Search for messages from this sender within ±10 seconds of timestamp
        val timestampWindow = 10_000L
        val selection = "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(
            sender,
            (timestamp - timestampWindow).toString(),
            (timestamp + timestampWindow).toString()
        )
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))

                return SmsMessage(
                    id = id,
                    address = address,
                    body = body,
                    timestamp = date
                )
            }
        }

        return null
    }

    /**
     * Data class for SMS message from AOSP database.
     */
    private data class SmsMessage(
        val id: Long,
        val address: String,
        val body: String,
        val timestamp: Long
    )
}
