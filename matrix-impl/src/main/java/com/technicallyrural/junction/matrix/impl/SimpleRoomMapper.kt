package com.technicallyrural.junction.matrix.impl

import android.content.Context
import android.content.SharedPreferences
import android.telephony.PhoneNumberUtils
import com.technicallyrural.junction.matrix.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId
import java.util.Locale

/**
 * Implementation of MatrixRoomMapper using SharedPreferences + Trixnity.
 *
 * Uses SharedPreferences for local mapping cache with Trixnity for:
 * - Room alias resolution
 * - Room creation with canonical aliases
 * - Room management
 *
 * Mappings stored with keys:
 * - "phone_to_room_<e164>" -> room ID
 * - "room_to_phone_<roomId>" -> E.164 phone number
 * - "room_alias_<e164>" -> room alias
 */
class SimpleRoomMapper(
    private val context: Context,
    private val clientManager: TrixnityClientManager,
    private val homeserverDomain: String
) : MatrixRoomMapper {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "matrix_room_mappings",
        Context.MODE_PRIVATE
    )

    private val mutex = Mutex()

    override suspend fun getRoomForContact(phoneNumber: String): String? = mutex.withLock {
        val client = clientManager.client ?: return null

        // 1. Normalize phone number to E.164
        val normalized = normalizeToE164(phoneNumber) ?: return null

        // 2. Check local cache
        val cached = prefs.getString(phoneKey(normalized), null)
        if (cached != null) {
            return cached
        }

        // 3. Try canonical room alias resolution
        val alias = buildRoomAlias(normalized)
        val roomByAlias = tryResolveAlias(alias)
        if (roomByAlias != null) {
            saveMapping(normalized, roomByAlias, alias)
            return roomByAlias
        }

        // 4. Create new DM room with alias
        return createRoomForContact(normalized, alias)
    }

    override suspend fun getContactForRoom(roomId: String): String? {
        return prefs.getString(roomKey(roomId), null)
    }

    override suspend fun getAllMappings(): List<RoomMapping> = mutex.withLock {
        val mappings = mutableListOf<RoomMapping>()
        val allPrefs = prefs.all

        for ((key, value) in allPrefs) {
            if (key.startsWith("phone_to_room_")) {
                val phone = key.removePrefix("phone_to_room_")
                val roomId = value as? String ?: continue
                val alias = prefs.getString(aliasKey(phone), null)

                mappings.add(
                    RoomMapping(
                        phoneNumber = phone,
                        roomId = roomId,
                        roomAlias = alias,
                        displayName = null,
                        createdAt = 0L,
                        lastSynced = 0L
                    )
                )
            }
        }

        return mappings
    }

    override suspend fun createMapping(
        phoneNumber: String,
        roomId: String,
        roomAlias: String?
    ): Unit = mutex.withLock {
        val normalized = normalizeToE164(phoneNumber) ?: return@withLock
        saveMapping(normalized, roomId, roomAlias)
    }

    override suspend fun deleteMapping(phoneNumber: String): Boolean = mutex.withLock {
        val normalized = normalizeToE164(phoneNumber) ?: return false
        val roomId = prefs.getString(phoneKey(normalized), null) ?: return false

        prefs.edit()
            .remove(phoneKey(normalized))
            .remove(roomKey(roomId))
            .remove(aliasKey(normalized))
            .apply()

        return true
    }

    override suspend fun syncMappings() {
        // No-op for SharedPreferences implementation
        // In Room version, this would verify rooms still exist
    }

    override suspend fun clearAllMappings() = mutex.withLock {
        prefs.edit().clear().apply()
    }

    /**
     * Save a phone→room mapping to SharedPreferences.
     */
    private fun saveMapping(phoneE164: String, roomId: String, roomAlias: String?) {
        prefs.edit()
            .putString(phoneKey(phoneE164), roomId)
            .putString(roomKey(roomId), phoneE164)
            .apply {
                if (roomAlias != null) {
                    putString(aliasKey(phoneE164), roomAlias)
                }
            }
            .apply()
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
                saveMapping(phoneE164, roomId.full, alias)
                roomId.full
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    // SharedPreferences key builders
    private fun phoneKey(phoneE164: String) = "phone_to_room_$phoneE164"
    private fun roomKey(roomId: String) = "room_to_phone_${roomId.replace(":", "_")}"
    private fun aliasKey(phoneE164: String) = "room_alias_$phoneE164"
}
