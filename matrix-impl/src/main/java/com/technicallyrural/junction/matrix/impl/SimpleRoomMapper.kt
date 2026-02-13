package com.technicallyrural.junction.matrix.impl

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.technicallyrural.junction.matrix.*
import com.technicallyrural.junction.persistence.repository.RoomMappingRepository
import com.technicallyrural.junction.persistence.util.AospThreadIdExtractor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId
import java.util.Locale

/**
 * Implementation of MatrixRoomMapper using Room database + Trixnity.
 *
 * Uses conversation-based mapping (not phone-based) to support:
 * - Same contact in multiple conversations (1:1 + different groups)
 * - Persistent storage across app restarts
 *
 * Mappings:
 * - Conversation ID (AOSP thread_id) → Matrix room ID
 * - Bidirectional lookup for bridging in both directions
 */
class SimpleRoomMapper(
    private val context: Context,
    private val clientManager: TrixnityClientManager,
    private val homeserverDomain: String
) : MatrixRoomMapper {

    private val roomRepo = RoomMappingRepository.getInstance(context)
    private val mutex = Mutex()

    companion object {
        private const val TAG = "SimpleRoomMapper"
    }

    override suspend fun getRoomForContact(phoneNumber: String): String? = mutex.withLock {
        val client = clientManager.client
        if (client == null) {
            Log.w(TAG, "Matrix client is null - not initialized yet")
            return null
        }

        // 1. Normalize phone number to E.164
        val normalized = normalizeToE164(phoneNumber) ?: return null

        // 2. Get conversation ID from AOSP thread system
        val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, normalized)

        // 3. Check database for existing mapping
        val cached = roomRepo.getRoomForConversation(conversationId)
        if (cached != null) {
            Log.d(TAG, "Room found in database for conversation $conversationId: $cached")
            return cached
        }

        // 4. Try canonical room alias resolution
        val alias = buildRoomAlias(normalized)
        val roomByAlias = tryResolveAlias(alias)
        if (roomByAlias != null) {
            // Save mapping
            roomRepo.setMapping(
                conversationId = conversationId,
                participants = listOf(normalized),
                roomId = roomByAlias,
                alias = alias,
                isGroup = false
            )
            return roomByAlias
        }

        // 5. Create new DM room with alias
        return createRoomForContact(conversationId, normalized, alias)
    }

    override suspend fun getContactForRoom(roomId: String): String? {
        // Get conversation ID from room mapping
        val conversationId = roomRepo.getConversationForRoom(roomId) ?: return null

        // Get participants for this conversation
        val participants = roomRepo.getParticipants(conversationId)

        // Return first participant (for 1:1 conversations)
        // TODO: Handle group conversations properly
        return participants?.firstOrNull()
    }

    override suspend fun getAllMappings(): List<RoomMapping> = mutex.withLock {
        val mappings = mutableListOf<RoomMapping>()
        val allMappings = roomRepo.getAllMappings()

        for (mapping in allMappings) {
            val participants = roomRepo.getParticipants(mapping.conversationId)
            val phoneNumber = participants?.firstOrNull() ?: continue

            mappings.add(
                RoomMapping(
                    phoneNumber = phoneNumber,
                    roomId = mapping.matrixRoomId,
                    roomAlias = mapping.matrixAlias,
                    displayName = null,
                    createdAt = mapping.createdAt,
                    lastSynced = mapping.lastUsed
                )
            )
        }

        return mappings
    }

    override suspend fun createMapping(
        phoneNumber: String,
        roomId: String,
        roomAlias: String?
    ): Unit = mutex.withLock {
        val normalized = normalizeToE164(phoneNumber) ?: return@withLock
        val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, normalized)

        roomRepo.setMapping(
            conversationId = conversationId,
            participants = listOf(normalized),
            roomId = roomId,
            alias = roomAlias,
            isGroup = false
        )
    }

    override suspend fun deleteMapping(phoneNumber: String): Boolean = mutex.withLock {
        val normalized = normalizeToE164(phoneNumber) ?: return false
        val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, normalized)

        roomRepo.removeMapping(conversationId)
        return true
    }

    override suspend fun syncMappings() {
        // TODO: Verify rooms still exist and clean up stale mappings
    }

    override suspend fun clearAllMappings() = mutex.withLock {
        roomRepo.clearAllMappings()
    }

    /**
     * Try to resolve a room alias to a room ID using Trixnity API.
     */
    private suspend fun tryResolveAlias(alias: String): String? {
        val client = clientManager.client ?: return null

        return try {
            val response = client.api.room.getRoomAlias(
                roomAliasId = RoomAliasId(alias)
            ).getOrNull()

            response?.roomId?.full
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a new DM room for a contact using Trixnity API.
     */
    private suspend fun createRoomForContact(
        conversationId: String,
        phoneE164: String,
        alias: String
    ): String? {
        val client = clientManager.client ?: return null

        return try {
            val roomId = client.api.room.createRoom(
                name = "SMS: $phoneE164",
                roomAliasId = RoomAliasId(alias),
                isDirect = true,
                invite = emptySet() // No invites needed for self-DM
            ).getOrElse { error ->
                // If alias creation fails, try without alias
                client.api.room.createRoom(
                    name = "SMS: $phoneE164",
                    isDirect = true
                ).getOrNull()
            }

            if (roomId != null) {
                // Save mapping
                roomRepo.setMapping(
                    conversationId = conversationId,
                    participants = listOf(phoneE164),
                    roomId = roomId.full,
                    alias = alias,
                    isGroup = false
                )
                Log.d(TAG, "Created room for conversation $conversationId: ${roomId.full}")
                roomId.full
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create room for conversation $conversationId", e)
            null
        }
    }

    /**
     * Build canonical room alias for a phone number.
     */
    private fun buildRoomAlias(phoneE164: String): String {
        val digits = phoneE164.replace("+", "")
        return "#sms_$digits:$homeserverDomain"
    }

    /**
     * Normalize phone number to E.164 format.
     */
    private fun normalizeToE164(phoneNumber: String): String? {
        if (phoneNumber.startsWith("+") && phoneNumber.length > 5) {
            return phoneNumber
        }

        val formatted = PhoneNumberUtils.formatNumberToE164(phoneNumber, Locale.getDefault().country)
        return formatted?.takeIf { it.startsWith("+") }
    }
}
