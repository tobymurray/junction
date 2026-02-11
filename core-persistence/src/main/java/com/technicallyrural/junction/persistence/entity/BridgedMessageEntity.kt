package com.technicallyrural.junction.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.technicallyrural.junction.persistence.model.Direction
import com.technicallyrural.junction.persistence.model.Status

/**
 * Persistent record of SMS ↔ Matrix message bridging with conversation context.
 *
 * Features:
 * - Conversation-aware deduplication (supports same contact in multiple conversations)
 * - Group message support (isGroup flag)
 * - Multi-participant tracking via MessageParticipantEntity
 *
 * Indices:
 * - dedupKey: Unique constraint for duplicate detection
 * - conversationId + timestamp: Query messages in specific conversation
 * - matrixEventId: Lookup by Matrix event for reverse mapping
 * - status: Find pending/failed messages for retry
 * - direction + status: Find pending outbound Matrix sends
 */
@Entity(
    tableName = "bridged_messages",
    indices = [
        Index(value = ["dedup_key"], unique = true),
        Index(value = ["conversation_id", "timestamp"]),
        Index(value = ["matrix_event_id"]),
        Index(value = ["status"]),
        Index(value = ["direction", "status"])
    ]
)
data class BridgedMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Deduplication key: SHA-256(conversationId|timestamp|bodyHash).
     *
     * Includes conversationId to distinguish:
     * - Same contact in different conversations (1:1 vs group)
     * - Same message content sent at same time to different groups
     */
    @ColumnInfo(name = "dedup_key")
    val dedupKey: String,

    /**
     * AOSP conversation thread ID.
     *
     * Identifies which conversation this message belongs to.
     * Format: String representation of AOSP thread_id (Long)
     */
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    /**
     * Message timestamp in epoch milliseconds.
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * SHA-256 hash of normalized message body (first 16 chars).
     */
    @ColumnInfo(name = "body_hash")
    val bodyHash: String,

    /**
     * Direction of message flow.
     */
    @ColumnInfo(name = "direction")
    val direction: Direction,

    /**
     * Whether this is a group conversation (2+ participants).
     */
    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    /**
     * AOSP message ID from BugleDatabase (nullable for Matrix→SMS until sent).
     */
    @ColumnInfo(name = "sms_message_id")
    val smsMessageId: Long? = null,

    /**
     * Matrix event ID (nullable for SMS→Matrix until confirmed).
     */
    @ColumnInfo(name = "matrix_event_id")
    val matrixEventId: String? = null,

    /**
     * Matrix room ID where message was sent/received.
     */
    @ColumnInfo(name = "matrix_room_id")
    val matrixRoomId: String? = null,

    /**
     * Current send status.
     */
    @ColumnInfo(name = "status")
    val status: Status,

    /**
     * Failure reason if status == FAILED (nullable).
     */
    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null,

    /**
     * Number of retry attempts.
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    /**
     * Record creation timestamp.
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /**
     * Last update timestamp.
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
