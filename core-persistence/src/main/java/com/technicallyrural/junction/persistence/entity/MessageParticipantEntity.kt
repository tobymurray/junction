package com.technicallyrural.junction.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

/**
 * Tracks participants (senders/recipients) for each message.
 *
 * Supports:
 * - Group MMS: Multiple recipients per message
 * - Group SMS: Multiple recipients per message
 * - Reverse lookup: Find all messages with specific participant
 */
@Entity(
    tableName = "message_participants",
    indices = [
        Index(value = ["message_id"]),
        Index(value = ["phone_number"]),
        Index(value = ["message_id", "phone_number"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = BridgedMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Foreign key to parent message.
     */
    @ColumnInfo(name = "message_id")
    val messageId: Long,

    /**
     * Phone number in E.164 format (e.g., +12345678901).
     */
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    /**
     * Participant role in this message.
     */
    @ColumnInfo(name = "participant_type")
    val participantType: ParticipantType
)

enum class ParticipantType {
    /**
     * Message sender (always single participant per message).
     */
    SENDER,

    /**
     * Message recipient (can be multiple for group messages).
     */
    RECIPIENT
}
