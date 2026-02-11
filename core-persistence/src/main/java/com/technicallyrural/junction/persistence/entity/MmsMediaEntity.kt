package com.technicallyrural.junction.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

/**
 * Persistent record of MMS media attachments.
 *
 * Tracks media upload/download status for MMS ↔ Matrix bridging.
 * Each media part is linked to a parent BridgedMessageEntity.
 *
 * Phase 1 implementation - Schema ready, implementation pending.
 */
@Entity(
    tableName = "mms_media",
    indices = [
        Index(value = ["message_id"]),
        Index(value = ["mxc_uri"]),
        Index(value = ["upload_status"])
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
data class MmsMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Foreign key to parent message.
     */
    @ColumnInfo(name = "message_id")
    val messageId: Long,

    /**
     * Local URI (content:// or file://).
     */
    @ColumnInfo(name = "local_uri")
    val localUri: String,

    /**
     * Matrix mxc:// URI (nullable until uploaded).
     */
    @ColumnInfo(name = "mxc_uri")
    val mxcUri: String? = null,

    /**
     * MIME type (e.g., image/jpeg, video/mp4).
     */
    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    /**
     * Original filename (nullable).
     */
    @ColumnInfo(name = "filename")
    val filename: String? = null,

    /**
     * File size in bytes.
     */
    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    /**
     * Upload/download status.
     */
    @ColumnInfo(name = "upload_status")
    val uploadStatus: UploadStatus,

    /**
     * Failure reason if upload_status == FAILED (nullable).
     */
    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null,

    /**
     * Record creation timestamp.
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

enum class UploadStatus {
    /** Queued for upload/download */
    PENDING,

    /** Upload/download in progress */
    IN_PROGRESS,

    /** Successfully uploaded to Matrix */
    UPLOADED,

    /** Successfully downloaded from Matrix */
    DOWNLOADED,

    /** Upload/download failed permanently */
    FAILED
}
