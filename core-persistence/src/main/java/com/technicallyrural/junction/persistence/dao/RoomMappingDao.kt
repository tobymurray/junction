package com.technicallyrural.junction.persistence.dao

import androidx.room.*
import com.technicallyrural.junction.persistence.entity.RoomMappingEntity

/**
 * Data Access Object for RoomMappingEntity.
 */
@Dao
interface RoomMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mapping: RoomMappingEntity): Long

    @Query("SELECT * FROM room_mappings WHERE conversation_id = :conversationId LIMIT 1")
    suspend fun findByConversationId(conversationId: String): RoomMappingEntity?

    @Query("SELECT * FROM room_mappings WHERE matrix_room_id = :roomId LIMIT 1")
    suspend fun findByRoomId(roomId: String): RoomMappingEntity?

    @Query("UPDATE room_mappings SET last_used = :timestamp WHERE conversation_id = :conversationId")
    suspend fun updateLastUsed(conversationId: String, timestamp: Long)

    @Query("SELECT * FROM room_mappings ORDER BY last_used DESC")
    suspend fun getAllMappings(): List<RoomMappingEntity>

    @Query("DELETE FROM room_mappings WHERE conversation_id = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)

    @Query("DELETE FROM room_mappings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM room_mappings")
    suspend fun count(): Int
}
