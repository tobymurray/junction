package com.technicallyrural.junction.persistence.dao

import androidx.room.*
import com.technicallyrural.junction.persistence.entity.MessageParticipantEntity

/**
 * Data Access Object for MessageParticipantEntity.
 */
@Dao
interface MessageParticipantDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(participant: MessageParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(participants: List<MessageParticipantEntity>)

    @Query("SELECT * FROM message_participants WHERE message_id = :messageId")
    suspend fun getParticipantsForMessage(messageId: Long): List<MessageParticipantEntity>

    @Query("""
        SELECT * FROM message_participants
        WHERE message_id = :messageId
        AND participant_type = 'SENDER'
        LIMIT 1
    """)
    suspend fun getSenderForMessage(messageId: Long): MessageParticipantEntity?

    @Query("""
        SELECT * FROM message_participants
        WHERE message_id = :messageId
        AND participant_type = 'RECIPIENT'
    """)
    suspend fun getRecipientsForMessage(messageId: Long): List<MessageParticipantEntity>

    @Query("""
        SELECT DISTINCT message_id FROM message_participants
        WHERE phone_number = :phoneNumber
        ORDER BY message_id DESC
        LIMIT :limit
    """)
    suspend fun findMessageIdsWithParticipant(
        phoneNumber: String,
        limit: Int = 100
    ): List<Long>

    @Query("DELETE FROM message_participants WHERE message_id = :messageId")
    suspend fun deleteForMessage(messageId: Long)
}
