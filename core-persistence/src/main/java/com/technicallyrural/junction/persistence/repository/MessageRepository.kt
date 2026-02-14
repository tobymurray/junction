package com.technicallyrural.junction.persistence.repository

import android.content.Context
import com.technicallyrural.junction.persistence.dao.BridgedMessageDao
import com.technicallyrural.junction.persistence.dao.MessageParticipantDao
import com.technicallyrural.junction.persistence.database.JunctionDatabase
import com.technicallyrural.junction.persistence.entity.BridgedMessageEntity
import com.technicallyrural.junction.persistence.entity.MessageParticipantEntity
import com.technicallyrural.junction.persistence.entity.ParticipantType
import com.technicallyrural.junction.persistence.model.Direction
import com.technicallyrural.junction.persistence.model.Status
import com.technicallyrural.junction.persistence.util.DedupKeyGenerator

/**
 * Repository for message bridging operations.
 *
 * Conversation-aware with multi-participant support.
 */
class MessageRepository(context: Context) {

    private val database = JunctionDatabase.getInstance(context)
    private val messageDao: BridgedMessageDao = database.bridgedMessageDao()
    private val participantDao: MessageParticipantDao = database.messageParticipantDao()

    companion object {
        private const val MAX_RETRIES = 5
        private const val TAG = "MessageRepository"

        @Volatile
        private var INSTANCE: MessageRepository? = null

        fun getInstance(context: Context): MessageRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MessageRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Record SMS → Matrix send attempt with conversation context.
     */
    suspend fun recordSmsToMatrixSend(
        conversationId: String,
        senderAddress: String,
        recipientAddresses: List<String>,
        body: String,
        timestamp: Long,
        isGroup: Boolean = recipientAddresses.size > 1,
        smsMessageId: Long? = null
    ): BridgedMessageEntity? {
        val dedupKey = DedupKeyGenerator.generate(conversationId, timestamp, body)

        // Check for duplicate
        if (messageDao.existsByDedupKey(dedupKey)) {
            android.util.Log.w(TAG, "Duplicate SMS → Matrix send detected: $dedupKey")
            return null
        }

        // Insert message record
        val entity = BridgedMessageEntity(
            dedupKey = dedupKey,
            conversationId = conversationId,
            timestamp = timestamp,
            bodyHash = DedupKeyGenerator.getBodyHash(body),
            direction = Direction.SMS_TO_MATRIX,
            isGroup = isGroup,
            smsMessageId = smsMessageId,
            status = Status.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val messageId = messageDao.insertOrIgnore(entity)
        if (messageId == -1L) return null

        // Insert participants
        val participants = mutableListOf<MessageParticipantEntity>()

        participants.add(MessageParticipantEntity(
            messageId = messageId,
            phoneNumber = senderAddress,
            participantType = ParticipantType.SENDER
        ))

        recipientAddresses.forEach { recipient ->
            participants.add(MessageParticipantEntity(
                messageId = messageId,
                phoneNumber = recipient,
                participantType = ParticipantType.RECIPIENT
            ))
        }

        participantDao.insertAll(participants)

        return entity.copy(id = messageId)
    }

    /**
     * Confirm Matrix send with event ID.
     */
    suspend fun confirmMatrixSend(
        dedupKey: String,
        matrixEventId: String,
        matrixRoomId: String
    ) {
        val existing = messageDao.findByDedupKey(dedupKey) ?: return

        messageDao.update(
            existing.copy(
                status = Status.CONFIRMED,
                matrixEventId = matrixEventId,
                matrixRoomId = matrixRoomId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Record Matrix send failure.
     */
    suspend fun recordMatrixSendFailure(
        dedupKey: String,
        failureReason: String
    ) {
        val existing = messageDao.findByDedupKey(dedupKey) ?: return
        val newRetryCount = existing.retryCount + 1

        messageDao.update(
            existing.copy(
                status = if (newRetryCount >= MAX_RETRIES) Status.FAILED else Status.PENDING,
                retryCount = newRetryCount,
                failureReason = failureReason,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Record Matrix → SMS send attempt.
     */
    suspend fun recordMatrixToSmsSend(
        matrixEventId: String,
        matrixRoomId: String,
        conversationId: String,
        senderAddress: String,
        recipientAddresses: List<String>,
        body: String,
        timestamp: Long,
        isGroup: Boolean = recipientAddresses.size > 1
    ): BridgedMessageEntity? {
        // Check for duplicate by Matrix event ID
        if (messageDao.existsByMatrixEventId(matrixEventId)) {
            android.util.Log.w(TAG, "Duplicate Matrix → SMS send detected: $matrixEventId")
            return null
        }

        val dedupKey = DedupKeyGenerator.generate(conversationId, timestamp, body)

        // Insert message record
        val entity = BridgedMessageEntity(
            dedupKey = dedupKey,
            conversationId = conversationId,
            timestamp = timestamp,
            bodyHash = DedupKeyGenerator.getBodyHash(body),
            direction = Direction.MATRIX_TO_SMS,
            isGroup = isGroup,
            matrixEventId = matrixEventId,
            matrixRoomId = matrixRoomId,
            status = Status.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val messageId = messageDao.insertOrIgnore(entity)
        if (messageId == -1L) return null

        // Insert participants
        val participants = mutableListOf<MessageParticipantEntity>()

        participants.add(MessageParticipantEntity(
            messageId = messageId,
            phoneNumber = senderAddress,
            participantType = ParticipantType.SENDER
        ))

        recipientAddresses.forEach { recipient ->
            participants.add(MessageParticipantEntity(
                messageId = messageId,
                phoneNumber = recipient,
                participantType = ParticipantType.RECIPIENT
            ))
        }

        participantDao.insertAll(participants)

        return entity.copy(id = messageId)
    }

    /**
     * Confirm SMS send with AOSP message ID.
     */
    suspend fun confirmSmsSend(
        matrixEventId: String,
        smsMessageId: Long
    ) {
        val existing = messageDao.findByMatrixEventId(matrixEventId) ?: return

        messageDao.update(
            existing.copy(
                status = Status.CONFIRMED,
                smsMessageId = smsMessageId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Record SMS send failure.
     */
    suspend fun recordSmsSendFailure(
        matrixEventId: String,
        failureReason: String
    ) {
        val existing = messageDao.findByMatrixEventId(matrixEventId) ?: return
        val newRetryCount = existing.retryCount + 1

        messageDao.update(
            existing.copy(
                status = if (newRetryCount >= MAX_RETRIES) Status.FAILED else Status.PENDING,
                retryCount = newRetryCount,
                failureReason = failureReason,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Get pending messages for retry (WorkManager integration).
     */
    suspend fun getPendingMessages(direction: Direction? = null): List<BridgedMessageEntity> {
        return if (direction != null) {
            messageDao.findPendingByDirection(direction, Status.PENDING, MAX_RETRIES)
        } else {
            messageDao.findPendingMessages(Status.PENDING, MAX_RETRIES)
        }
    }

    /**
     * Get messages for a conversation.
     */
    suspend fun getMessagesForConversation(
        conversationId: String,
        limit: Int = 100
    ): List<BridgedMessageEntity> {
        return messageDao.getMessagesForConversation(conversationId, limit)
    }

    /**
     * Get participants for a message.
     */
    suspend fun getParticipants(messageId: Long): List<MessageParticipantEntity> {
        return participantDao.getParticipantsForMessage(messageId)
    }

    /**
     * Cleanup old confirmed messages (retain last N days).
     */
    suspend fun cleanupOldMessages(retentionDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        messageDao.deleteOldConfirmed(cutoff)
    }

    /**
     * Get metrics (for monitoring).
     */
    suspend fun getMetrics(): Map<Status, Int> {
        return mapOf(
            Status.PENDING to messageDao.countByStatus(Status.PENDING),
            Status.SENT to messageDao.countByStatus(Status.SENT),
            Status.CONFIRMED to messageDao.countByStatus(Status.CONFIRMED),
            Status.FAILED to messageDao.countByStatus(Status.FAILED)
        )
    }

    /**
     * Record outbound Phone → Matrix send (user-sent messages).
     *
     * This is for messages sent BY the user (from this phone) that need to be
     * bridged to Matrix. Different from recordSmsToMatrixSend() which handles
     * inbound SMS from external senders.
     *
     * @param smsMessageId The AOSP telephony database message ID (primary dedup key)
     * @param conversationId The AOSP thread ID
     * @param senderAddress Our phone number (self)
     * @param recipientAddresses Destination phone numbers
     * @param body Message text
     * @param timestamp Send timestamp
     * @return Entity if created, null if duplicate
     */
    suspend fun recordPhoneToMatrixSend(
        smsMessageId: Long,
        conversationId: String,
        senderAddress: String,
        recipientAddresses: List<String>,
        body: String,
        timestamp: Long,
        isGroup: Boolean = recipientAddresses.size > 1
    ): BridgedMessageEntity? {
        // Primary deduplication: SMS message ID from AOSP database
        if (messageDao.existsBySmsMessageId(smsMessageId)) {
            android.util.Log.w(TAG, "Duplicate Phone → Matrix send detected: smsMessageId=$smsMessageId")
            return null
        }

        // Secondary deduplication: conversation + timestamp + body hash
        val dedupKey = DedupKeyGenerator.generate(conversationId, timestamp, body)
        if (messageDao.existsByDedupKey(dedupKey)) {
            android.util.Log.w(TAG, "Duplicate Phone → Matrix send detected: dedupKey=$dedupKey")
            return null
        }

        // Insert message record
        val entity = BridgedMessageEntity(
            dedupKey = dedupKey,
            conversationId = conversationId,
            timestamp = timestamp,
            bodyHash = DedupKeyGenerator.getBodyHash(body),
            direction = Direction.SMS_TO_MATRIX,
            isGroup = isGroup,
            smsMessageId = smsMessageId,
            status = Status.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val messageId = messageDao.insertOrIgnore(entity)
        if (messageId == -1L) return null

        // Insert participants
        val participants = mutableListOf<MessageParticipantEntity>()

        participants.add(MessageParticipantEntity(
            messageId = messageId,
            phoneNumber = senderAddress,
            participantType = ParticipantType.SENDER
        ))

        recipientAddresses.forEach { recipient ->
            participants.add(MessageParticipantEntity(
                messageId = messageId,
                phoneNumber = recipient,
                participantType = ParticipantType.RECIPIENT
            ))
        }

        participantDao.insertAll(participants)

        return entity.copy(id = messageId)
    }

    /**
     * Check if a message has been bridged (by AOSP SMS message ID).
     */
    suspend fun existsBySmsMessageId(smsMessageId: Long): Boolean {
        return messageDao.existsBySmsMessageId(smsMessageId)
    }

    /**
     * Get bridge status for a message (by AOSP SMS message ID).
     */
    suspend fun getBridgeStatus(smsMessageId: Long): Status? {
        return messageDao.findBySmsMessageId(smsMessageId)?.status
    }
}
