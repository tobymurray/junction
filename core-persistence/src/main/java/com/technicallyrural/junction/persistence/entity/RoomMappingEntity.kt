package com.technicallyrural.junction.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Persistent mapping between AOSP conversation threads and Matrix room IDs.
 *
 * Conversation-based mapping (not phone-based) to support:
 * - Same contact in multiple conversations (1:1 + different groups)
 * - Thread 1 (1:1: Me ↔ Alice) → Room !abc:server.com
 * - Thread 2 (Group: Me ↔ Alice ↔ Bob) → Room !xyz:server.com
 */
@Entity(
    tableName = "room_mappings",
    indices = [
        Index(value = ["conversation_id"], unique = true),
        Index(value = ["matrix_room_id"], unique = true),
        Index(value = ["last_used"])
    ]
)
data class RoomMappingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * AOSP conversation thread ID.
     * Format: String representation of AOSP thread_id (Long)
     */
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    /**
     * Participants in this conversation (JSON array of E.164 phone numbers).
     *
     * Examples:
     * - 1:1: ["+12345678901"]
     * - Group: ["+12345678901", "+19876543210", "+15555555555"]
     *
     * Array is sorted for consistent comparison.
     */
    @ColumnInfo(name = "participants_json")
    val participantsJson: String,

    /**
     * Matrix room ID (e.g., !abc123:server.com).
     */
    @ColumnInfo(name = "matrix_room_id")
    val matrixRoomId: String,

    /**
     * Matrix room alias if created with alias (nullable).
     */
    @ColumnInfo(name = "matrix_alias")
    val matrixAlias: String? = null,

    /**
     * Whether this is a group conversation (2+ participants).
     */
    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    /**
     * Last time this mapping was used (epoch millis).
     */
    @ColumnInfo(name = "last_used")
    val lastUsed: Long,

    /**
     * Mapping creation timestamp.
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
