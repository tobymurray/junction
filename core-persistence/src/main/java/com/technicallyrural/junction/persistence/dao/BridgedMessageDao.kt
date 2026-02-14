package com.technicallyrural.junction.persistence.dao

import androidx.room.*
import com.technicallyrural.junction.persistence.entity.BridgedMessageEntity
import com.technicallyrural.junction.persistence.model.Direction
import com.technicallyrural.junction.persistence.model.Status

/**
 * Data Access Object for BridgedMessageEntity.
 */
@Dao
interface BridgedMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(message: BridgedMessageEntity): Long

    @Update
    suspend fun update(message: BridgedMessageEntity)

    @Query("SELECT * FROM bridged_messages WHERE dedup_key = :dedupKey LIMIT 1")
    suspend fun findByDedupKey(dedupKey: String): BridgedMessageEntity?

    @Query("SELECT * FROM bridged_messages WHERE sms_message_id = :smsMessageId LIMIT 1")
    suspend fun findBySmsMessageId(smsMessageId: Long): BridgedMessageEntity?

    @Query("SELECT * FROM bridged_messages WHERE matrix_event_id = :eventId LIMIT 1")
    suspend fun findByMatrixEventId(eventId: String): BridgedMessageEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bridged_messages WHERE dedup_key = :dedupKey)")
    suspend fun existsByDedupKey(dedupKey: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bridged_messages WHERE matrix_event_id = :eventId)")
    suspend fun existsByMatrixEventId(eventId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bridged_messages WHERE sms_message_id = :smsMessageId)")
    suspend fun existsBySmsMessageId(smsMessageId: Long): Boolean

    @Query("""
        SELECT * FROM bridged_messages
        WHERE status = :status
        AND retry_count < :maxRetries
        ORDER BY created_at ASC
    """)
    suspend fun findPendingMessages(
        status: Status = Status.PENDING,
        maxRetries: Int = 5
    ): List<BridgedMessageEntity>

    @Query("""
        SELECT * FROM bridged_messages
        WHERE direction = :direction
        AND status = :status
        AND retry_count < :maxRetries
        ORDER BY created_at ASC
    """)
    suspend fun findPendingByDirection(
        direction: Direction,
        status: Status = Status.PENDING,
        maxRetries: Int = 5
    ): List<BridgedMessageEntity>

    @Query("""
        SELECT * FROM bridged_messages
        WHERE conversation_id = :conversationId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getMessagesForConversation(
        conversationId: String,
        limit: Int = 100
    ): List<BridgedMessageEntity>

    @Query("""
        DELETE FROM bridged_messages
        WHERE status = 'CONFIRMED'
        AND updated_at < :cutoffTimestamp
    """)
    suspend fun deleteOldConfirmed(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM bridged_messages WHERE status = :status")
    suspend fun countByStatus(status: Status): Int
}
