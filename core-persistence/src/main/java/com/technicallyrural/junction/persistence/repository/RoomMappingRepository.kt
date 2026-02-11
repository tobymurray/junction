package com.technicallyrural.junction.persistence.repository

import android.content.Context
import com.technicallyrural.junction.persistence.dao.RoomMappingDao
import com.technicallyrural.junction.persistence.database.JunctionDatabase
import com.technicallyrural.junction.persistence.entity.RoomMappingEntity
import com.technicallyrural.junction.persistence.util.ParticipantsSerializer

/**
 * Repository for conversation ↔ Matrix room mappings.
 *
 * Conversation-based mapping (not phone-based).
 */
class RoomMappingRepository(context: Context) {

    private val dao: RoomMappingDao =
        JunctionDatabase.getInstance(context).roomMappingDao()

    companion object {
        @Volatile
        private var INSTANCE: RoomMappingRepository? = null

        fun getInstance(context: Context): RoomMappingRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = RoomMappingRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Get Matrix room ID for conversation.
     */
    suspend fun getRoomForConversation(conversationId: String): String? {
        val mapping = dao.findByConversationId(conversationId)
        if (mapping != null) {
            dao.updateLastUsed(conversationId, System.currentTimeMillis())
        }
        return mapping?.matrixRoomId
    }

    /**
     * Get conversation ID for Matrix room.
     */
    suspend fun getConversationForRoom(roomId: String): String? {
        return dao.findByRoomId(roomId)?.conversationId
    }

    /**
     * Get participants for a conversation.
     */
    suspend fun getParticipants(conversationId: String): List<String>? {
        val mapping = dao.findByConversationId(conversationId) ?: return null
        return ParticipantsSerializer.deserialize(mapping.participantsJson)
    }

    /**
     * Store or update conversation → room mapping.
     */
    suspend fun setMapping(
        conversationId: String,
        participants: List<String>,
        roomId: String,
        alias: String? = null,
        isGroup: Boolean = participants.size > 1
    ) {
        val participantsJson = ParticipantsSerializer.serialize(participants)

        dao.upsert(
            RoomMappingEntity(
                conversationId = conversationId,
                participantsJson = participantsJson,
                matrixRoomId = roomId,
                matrixAlias = alias,
                isGroup = isGroup,
                lastUsed = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Remove mapping for conversation.
     */
    suspend fun removeMapping(conversationId: String) {
        dao.deleteByConversationId(conversationId)
    }

    /**
     * Remove all mappings (SIM swap scenario).
     */
    suspend fun clearAllMappings() {
        dao.deleteAll()
    }

    /**
     * Get all mappings (for export/debugging).
     */
    suspend fun getAllMappings(): List<RoomMappingEntity> {
        return dao.getAllMappings()
    }

    /**
     * Get mapping count.
     */
    suspend fun getMappingCount(): Int {
        return dao.count()
    }
}
