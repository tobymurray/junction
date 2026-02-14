package com.technicallyrural.junction.matrix.impl

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.technicallyrural.junction.matrix.*
import com.technicallyrural.junction.matrix.impl.shortcode.ServiceClassifier
import com.technicallyrural.junction.matrix.impl.shortcode.ServiceRoomMapper
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
 * - Service-based grouping for short codes (when enabled)
 *
 * Mappings:
 * - Conversation ID (AOSP thread_id) → Matrix room ID (regular numbers)
 * - Service ID (service:$key) → Matrix room ID (grouped short codes)
 * - Bidirectional lookup for bridging in both directions
 *
 * @param enableServiceGrouping Whether to group short codes by service (default: true)
 */
class SimpleRoomMapper(
    private val context: Context,
    private val clientManager: TrixnityClientManager,
    private val homeserverDomain: String,
    private val enableServiceGrouping: Boolean = true
) : MatrixRoomMapper {

    private val roomRepo = RoomMappingRepository.getInstance(context)
    private val mutex = Mutex()

    // Service classification components (lazy initialization)
    private val serviceClassifier by lazy { ServiceClassifier(context) }
    private val serviceRoomMapper by lazy { ServiceRoomMapper(context, clientManager, homeserverDomain) }

    companion object {
        private const val TAG = "SimpleRoomMapper"
    }

    override suspend fun getRoomForContact(
        phoneNumber: String,
        messageBody: String?,
        timestamp: Long
    ): String? = mutex.withLock {
        Log.d(TAG, "getRoomForContact called for: $phoneNumber (hasBody=${messageBody != null})")

        val client = clientManager.client
        if (client == null) {
            Log.e(TAG, "Matrix client is NULL - not initialized yet!")
            return@withLock null
        }

        // 1. Normalize phone number to E.164 or detect short code
        val normalized = normalizeToE164(phoneNumber)
        if (normalized == null) {
            Log.e(TAG, "Phone number normalization returned null for: $phoneNumber")
            return@withLock null
        }
        Log.d(TAG, "Phone normalized to: $normalized")

        // 2. Check if this is a short code with service grouping enabled
        if (normalized.startsWith("short:") && messageBody != null && enableServiceGrouping) {
            Log.d(TAG, "Service grouping enabled, classifying short code")
            return getGroupedShortCodeRoom(normalized, messageBody, timestamp)
        } else if (normalized.startsWith("short:")) {
            Log.d(TAG, "Service grouping disabled or no message body, using per-number mapping")
        }

        // 3. Standard per-number mapping path (regular numbers or grouping disabled)
        val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, normalized)

        // 4. Check database for existing mapping
        val cached = roomRepo.getRoomForConversation(conversationId)
        if (cached != null) {
            Log.d(TAG, "Room found in database for conversation $conversationId: $cached")
            return cached
        }

        // 5. Try canonical room alias resolution
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

        // 6. Create new DM room with alias
        return createRoomForContact(conversationId, normalized, alias)
    }

    /**
     * Get or create service-grouped room for short code.
     *
     * Strategy:
     * 1. Check if short code is already mapped to a service room (from previous messages)
     * 2. If yes, reuse that room (ensures multi-part SMS and outbound messages use same room)
     * 3. If no, classify message and create/find appropriate room
     */
    private suspend fun getGroupedShortCodeRoom(
        normalizedNumber: String,
        messageBody: String,
        timestamp: Long
    ): String? {
        val shortCode = normalizedNumber.removePrefix("short:")

        // STEP 1: Check if this short code is already associated with a service room
        // This ensures multi-part SMS and outbound messages use the same room
        val existingServiceRoom = findExistingServiceRoomForShortCode(normalizedNumber)
        if (existingServiceRoom != null) {
            Log.d(TAG, "Reusing existing service room for short code $shortCode: $existingServiceRoom")
            return existingServiceRoom
        }

        // STEP 2: No existing mapping, classify message by service
        val classification = serviceClassifier.classifyMessage(shortCode, messageBody, timestamp)

        Log.d(TAG, "Classification result: ${classification.serviceKey} " +
                "(confidence=${classification.confidence}, reason=${classification.reason})")

        // STEP 3: If classification failed or returned per-number fallback, use standard mapping
        if (classification.serviceKey.startsWith("unknown_")) {
            Log.d(TAG, "Using per-number mapping for unknown short code $shortCode")
            val conversationId = AospThreadIdExtractor.getThreadIdForAddress(context, normalizedNumber)
            val cached = roomRepo.getRoomForConversation(conversationId)
            if (cached != null) return cached

            val alias = buildRoomAlias(normalizedNumber)
            val roomByAlias = tryResolveAlias(alias)
            if (roomByAlias != null) {
                roomRepo.setMapping(
                    conversationId = conversationId,
                    participants = listOf(normalizedNumber),
                    roomId = roomByAlias,
                    alias = alias,
                    isGroup = false
                )
                return roomByAlias
            }

            return createRoomForContact(conversationId, normalizedNumber, alias)
        }

        // STEP 4: Get or create service room
        return serviceRoomMapper.getServiceRoom(
            serviceKey = classification.serviceKey,
            serviceName = classification.serviceName,
            shortCode = shortCode
        )
    }

    /**
     * Find existing service room that this short code is already associated with.
     *
     * Searches all service rooms (conversationId starts with "service:") and checks
     * if this short code is in the participants list.
     *
     * This ensures:
     * - Multi-part SMS messages go to the same room (even if later parts don't match pattern)
     * - Outbound messages use the same room as inbound messages
     */
    private suspend fun findExistingServiceRoomForShortCode(normalizedNumber: String): String? {
        try {
            val allMappings = roomRepo.getAllMappings()

            // Find service rooms (conversationId = "service:$key")
            for (mapping in allMappings) {
                if (mapping.conversationId.startsWith("service:")) {
                    val participants = roomRepo.getParticipants(mapping.conversationId)
                    if (participants?.contains(normalizedNumber) == true) {
                        Log.d(TAG, "Found existing service room for $normalizedNumber: " +
                                "${mapping.conversationId} → ${mapping.matrixRoomId}")
                        return mapping.matrixRoomId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for existing service room", e)
        }

        return null
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
