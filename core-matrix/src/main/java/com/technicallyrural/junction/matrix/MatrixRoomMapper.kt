package com.technicallyrural.junction.matrix

/**
 * Interface for mapping phone numbers to Matrix rooms.
 *
 * This handles the bidirectional mapping between SMS contacts (phone numbers)
 * and Matrix rooms. The implementation uses a hybrid approach:
 * 1. Try canonical room alias (e.g., #sms_15550100:server.org)
 * 2. Check local database cache
 * 3. Create new DM room with alias if needed
 *
 * Implementation is provided by matrix-impl module.
 */
interface MatrixRoomMapper {

    /**
     * Get or create the Matrix room for a phone number.
     *
     * This method:
     * 1. Normalizes the phone number to E.164 format
     * 2. For short codes with service grouping enabled, classifies by message content
     * 3. Checks the local database cache (per-number or service-based)
     * 4. Tries to resolve the canonical room alias
     * 5. Creates a new DM room if no mapping exists
     * 6. Stores the mapping in the local database
     *
     * @param phoneNumber Phone number (will be normalized to E.164)
     * @param messageBody Message content for service classification (optional for regular numbers, required for short code grouping)
     * @param timestamp Message timestamp for time-based classification patterns
     * @return Room ID for the contact, or null if creation failed
     */
    suspend fun getRoomForContact(
        phoneNumber: String,
        messageBody: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): String?

    /**
     * Get the phone number associated with a Matrix room.
     *
     * This is the reverse lookup for processing incoming Matrix messages.
     *
     * @param roomId Matrix room ID
     * @return E.164 formatted phone number, or null if room is not mapped
     */
    suspend fun getContactForRoom(roomId: String): String?

    /**
     * Get all currently mapped phone numbers.
     *
     * Useful for displaying active bridges in the UI.
     *
     * @return List of room mappings
     */
    suspend fun getAllMappings(): List<RoomMapping>

    /**
     * Manually create a mapping between a phone number and room.
     *
     * This is useful for:
     * - Importing existing room mappings from another device
     * - Testing
     * - Manual room assignment in the UI
     *
     * @param phoneNumber E.164 formatted phone number
     * @param roomId Matrix room ID
     * @param roomAlias Optional room alias
     */
    suspend fun createMapping(
        phoneNumber: String,
        roomId: String,
        roomAlias: String? = null
    )

    /**
     * Delete a room mapping.
     *
     * This does NOT delete the Matrix room itself, only the local mapping.
     *
     * @param phoneNumber E.164 formatted phone number
     * @return true if mapping was deleted
     */
    suspend fun deleteMapping(phoneNumber: String): Boolean

    /**
     * Sync room mappings from the Matrix server.
     *
     * This queries the server for all rooms with the canonical alias pattern
     * and updates the local database. Useful for multi-device sync.
     */
    suspend fun syncMappings()

    /**
     * Clear all room mappings from the local database.
     *
     * This is a destructive operation used for logout or reset.
     */
    suspend fun clearAllMappings()
}

/**
 * A mapping between a phone number and a Matrix room.
 */
data class RoomMapping(
    val phoneNumber: String,      // E.164 format
    val roomId: String,            // Matrix room ID (!xyz:server)
    val roomAlias: String?,        // Optional canonical alias (#sms_...:server)
    val displayName: String?,      // Contact name if resolved
    val createdAt: Long,           // Timestamp when mapping was created
    val lastSynced: Long           // Timestamp of last successful sync
)

/**
 * Configuration for room creation.
 */
data class RoomCreationConfig(
    val isDirect: Boolean = true,
    val preset: RoomPreset = RoomPreset.TRUSTED_PRIVATE_CHAT,
    val inviteUserIds: List<String> = emptyList(),
    val enableEncryption: Boolean = true
)

/**
 * Matrix room preset for creation.
 */
enum class RoomPreset {
    PRIVATE_CHAT,
    TRUSTED_PRIVATE_CHAT,
    PUBLIC_CHAT
}
