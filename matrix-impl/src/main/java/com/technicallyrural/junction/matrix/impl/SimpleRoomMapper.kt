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
        Log.e(TAG, "getRoomForContact called for: $phoneNumber")

        val client = clientManager.client
        if (client == null) {
            Log.e(TAG, "Matrix client is NULL - not initialized yet!")
            return@withLock null
        }

        Log.e(TAG, "Matrix client is available, proceeding...")

        // 1. Normalize phone number to E.164 or detect short code
        val normalized = normalizeToE164(phoneNumber)
        if (normalized == null) {
            Log.e(TAG, "Phone number normalization returned null for: $phoneNumber (should not happen)")
            return@withLock null
        }
        Log.e(TAG, "Phone normalized to: $normalized")

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
        normalizedNumber: String,
        alias: String
    ): String? {
        val client = clientManager.client ?: return null

        // Create descriptive room name based on number type
        val roomName = when {
            normalizedNumber.startsWith("short:") ->
                "SMS Short Code: ${normalizedNumber.removePrefix("short:")}"
            normalizedNumber.startsWith("unknown:") ->
                "SMS Unknown: ${normalizedNumber.removePrefix("unknown:")}"
            else ->
                "SMS: $normalizedNumber"
        }

        return try {
            val roomId = client.api.room.createRoom(
                name = roomName,
                roomAliasId = RoomAliasId(alias),
                isDirect = true,
                invite = emptySet() // No invites needed for self-DM
            ).getOrElse { error ->
                // If alias creation fails, try without alias
                client.api.room.createRoom(
                    name = roomName,
                    isDirect = true
                ).getOrNull()
            }

            if (roomId != null) {
                // Save mapping
                roomRepo.setMapping(
                    conversationId = conversationId,
                    participants = listOf(normalizedNumber),
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
     * Build canonical room alias for a phone number or short code.
     *
     * Examples:
     * - "+16138584798" -> "#sms_16138584798:homeserver.com"
     * - "short:83687" -> "#sms_short_83687:homeserver.com"
     * - "unknown:ALERTS" -> "#sms_unknown_alerts:homeserver.com"
     */
    private fun buildRoomAlias(normalizedNumber: String): String {
        val sanitized = normalizedNumber
            .replace("+", "")
            .replace(":", "_")
            .lowercase()
        return "#sms_$sanitized:$homeserverDomain"
    }

    /**
     * Normalize phone number to E.164 format, or handle as short code.
     *
     * Short codes are 3-8 digit numbers used for services like:
     * - Two-factor authentication (2FA)
     * - Marketing messages
     * - Emergency alerts
     * - Service notifications
     *
     * Returns:
     * - E.164 format for regular phone numbers (e.g., "+16138584798")
     * - Short code with "short:" prefix (e.g., "short:83687")
     * - Original number with "unknown:" prefix if normalization fails
     */
    private fun normalizeToE164(phoneNumber: String): String? {
        // Already in E.164 format
        if (phoneNumber.startsWith("+") && phoneNumber.length > 5) {
            Log.d(TAG, "Number already in E.164 format: $phoneNumber")
            return phoneNumber
        }

        // Check if this is a short code (3-8 digits only, no country code)
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length in 3..8 && digitsOnly == phoneNumber) {
            val shortCodeId = "short:$phoneNumber"
            Log.d(TAG, "Detected short code: $phoneNumber -> $shortCodeId")
            return shortCodeId
        }

        // Try E.164 normalization for regular phone numbers
        val formatted = PhoneNumberUtils.formatNumberToE164(phoneNumber, Locale.getDefault().country)
        if (formatted != null && formatted.startsWith("+")) {
            Log.d(TAG, "Normalized to E.164: $phoneNumber -> $formatted")
            return formatted
        }

        // Fallback: Use original number with prefix to avoid conflicts
        // This handles cases like alphanumeric sender IDs or unusual formats
        val fallbackId = "unknown:$phoneNumber"
        Log.w(TAG, "Failed to normalize, using fallback: $phoneNumber -> $fallbackId")
        return fallbackId
    }
}
