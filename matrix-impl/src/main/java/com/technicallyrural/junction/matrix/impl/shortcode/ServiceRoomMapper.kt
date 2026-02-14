package com.technicallyrural.junction.matrix.impl.shortcode

import android.content.Context
import android.util.Log
import com.technicallyrural.junction.matrix.impl.TrixnityClientManager
import com.technicallyrural.junction.persistence.repository.RoomMappingRepository
import de.connect2x.trixnity.core.model.RoomAliasId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Maps service keys to persistent Matrix rooms.
 *
 * Storage strategy:
 * - Reuses existing RoomMappingEntity table
 * - Convention: conversationId = "service:$serviceKey"
 * - Participants field stores list of short codes seen for this service
 * - Enables rollback (toggle off returns to per-number mapping)
 */
class ServiceRoomMapper(
    private val context: Context,
    private val clientManager: TrixnityClientManager,
    private val homeserverDomain: String
) {
    private val roomRepo = RoomMappingRepository.getInstance(context)
    private val mutex = Mutex()

    companion object {
        private const val TAG = "ServiceRoomMapper"
        private const val SERVICE_CONVERSATION_PREFIX = "service:"
    }

    /**
     * Get or create a Matrix room for a service.
     *
     * @param serviceKey Service identifier (e.g., "google_verify", "tangerine_bank")
     * @param serviceName Human-readable service name (e.g., "Google Verification")
     * @param shortCode The specific short code that sent this message
     * @return Matrix room ID, or null if creation failed
     */
    suspend fun getServiceRoom(
        serviceKey: String,
        serviceName: String,
        shortCode: String
    ): String? = mutex.withLock {
        val conversationId = "$SERVICE_CONVERSATION_PREFIX$serviceKey"
        Log.d(TAG, "Getting service room for $serviceKey (short code: $shortCode)")

        // 1. Check database cache
        val cached = roomRepo.getRoomForConversation(conversationId)
        if (cached != null) {
            Log.d(TAG, "Found cached service room for $serviceKey: $cached")
            // Update participants list to include this short code
            addShortCodeToService(conversationId, shortCode)
            return cached
        }

        // 2. Try canonical alias resolution
        val alias = buildServiceAlias(serviceKey)
        val roomByAlias = tryResolveAlias(alias)
        if (roomByAlias != null) {
            Log.d(TAG, "Resolved service room via alias for $serviceKey: $roomByAlias")
            roomRepo.setMapping(
                conversationId = conversationId,
                participants = listOf("short:$shortCode"),
                roomId = roomByAlias,
                alias = alias,
                isGroup = false
            )
            return roomByAlias
        }

        // 3. Create new service room
        return createServiceRoom(conversationId, serviceKey, serviceName, shortCode, alias)
    }

    /**
     * Create a new Matrix room for a service.
     */
    private suspend fun createServiceRoom(
        conversationId: String,
        serviceKey: String,
        serviceName: String,
        shortCode: String,
        alias: String
    ): String? {
        val client = clientManager.client
        if (client == null) {
            Log.e(TAG, "Matrix client is null, cannot create service room")
            return null
        }

        return try {
            Log.d(TAG, "Creating new service room for $serviceKey with alias $alias")

            val roomId = client.api.room.createRoom(
                name = serviceName,  // e.g., "Google Verification"
                roomAliasId = RoomAliasId(alias),
                isDirect = true,
                topic = "Automated service messages from SMS short codes"
            ).getOrElse { error ->
                // Fallback: try without alias if alias creation fails
                Log.w(TAG, "Failed to create room with alias, trying without: $error")
                client.api.room.createRoom(
                    name = serviceName,
                    isDirect = true,
                    topic = "Automated service messages from SMS short codes"
                ).getOrNull()
            }

            if (roomId != null) {
                // Save mapping with service convention
                roomRepo.setMapping(
                    conversationId = conversationId,
                    participants = listOf("short:$shortCode"),
                    roomId = roomId.full,
                    alias = alias,
                    isGroup = false
                )
                Log.d(TAG, "Created service room for $serviceKey: ${roomId.full}")
                roomId.full
            } else {
                Log.e(TAG, "Failed to create service room for $serviceKey")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating service room for $serviceKey", e)
            null
        }
    }

    /**
     * Build canonical room alias for a service.
     *
     * Examples:
     * - "google_verify" -> "#sms_service_google_verify:homeserver.com"
     * - "tangerine_bank" -> "#sms_service_tangerine_bank:homeserver.com"
     */
    private fun buildServiceAlias(serviceKey: String): String {
        val sanitized = serviceKey.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        return "#sms_service_$sanitized:$homeserverDomain"
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
            Log.d(TAG, "Alias resolution failed for $alias (expected if room doesn't exist yet)")
            null
        }
    }

    /**
     * Add a short code to the participants list for a service room.
     *
     * This tracks which short codes have been seen for this service.
     * Useful for debugging and potential UI display.
     */
    private suspend fun addShortCodeToService(conversationId: String, shortCode: String) {
        try {
            val mapping = roomRepo.getAllMappings().find { it.conversationId == conversationId }
            if (mapping != null) {
                val participants = roomRepo.getParticipants(conversationId) ?: emptyList()
                val shortCodeId = "short:$shortCode"

                if (shortCodeId !in participants) {
                    Log.d(TAG, "Adding short code $shortCode to service ${conversationId.removePrefix(SERVICE_CONVERSATION_PREFIX)}")
                    roomRepo.setMapping(
                        conversationId = conversationId,
                        participants = participants + shortCodeId,
                        roomId = mapping.matrixRoomId,
                        alias = mapping.matrixAlias,
                        isGroup = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add short code to service participants", e)
            // Non-critical failure, don't propagate
        }
    }

    /**
     * Get all service rooms (for debugging/admin UI).
     *
     * @return List of (serviceKey, roomId) pairs
     */
    suspend fun getAllServiceRooms(): List<Pair<String, String>> {
        return roomRepo.getAllMappings()
            .filter { it.conversationId.startsWith(SERVICE_CONVERSATION_PREFIX) }
            .map { mapping ->
                val serviceKey = mapping.conversationId.removePrefix(SERVICE_CONVERSATION_PREFIX)
                serviceKey to mapping.matrixRoomId
            }
    }

    /**
     * Remove a service room mapping (admin function).
     *
     * Does NOT delete the Matrix room itself, only the local mapping.
     * New messages from this service will create a new room.
     */
    suspend fun removeServiceRoom(serviceKey: String) {
        val conversationId = "$SERVICE_CONVERSATION_PREFIX$serviceKey"
        Log.d(TAG, "Removing service room mapping for $serviceKey")
        roomRepo.removeMapping(conversationId)
    }

    /**
     * Remove all service room mappings (for rollback/reset).
     *
     * Does NOT delete the Matrix rooms themselves.
     */
    suspend fun removeAllServiceRooms() {
        Log.d(TAG, "Removing all service room mappings")
        val serviceMappings = roomRepo.getAllMappings()
            .filter { it.conversationId.startsWith(SERVICE_CONVERSATION_PREFIX) }

        serviceMappings.forEach { mapping ->
            roomRepo.removeMapping(mapping.conversationId)
        }

        Log.d(TAG, "Removed ${serviceMappings.size} service room mappings")
    }
}
