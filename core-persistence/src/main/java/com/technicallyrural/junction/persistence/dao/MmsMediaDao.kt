package com.technicallyrural.junction.persistence.dao

import androidx.room.*
import com.technicallyrural.junction.persistence.entity.MmsMediaEntity
import com.technicallyrural.junction.persistence.entity.UploadStatus

/**
 * Data Access Object for MmsMediaEntity.
 */
@Dao
interface MmsMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MmsMediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<MmsMediaEntity>)

    @Update
    suspend fun update(media: MmsMediaEntity)

    @Query("SELECT * FROM mms_media WHERE message_id = :messageId ORDER BY id ASC")
    suspend fun getMediaForMessage(messageId: Long): List<MmsMediaEntity>

    @Query("""
        SELECT * FROM mms_media
        WHERE upload_status = :status
        ORDER BY created_at ASC
    """)
    suspend fun findByStatus(status: UploadStatus): List<MmsMediaEntity>

    @Query("UPDATE mms_media SET upload_status = :status, mxc_uri = :mxcUri WHERE id = :id")
    suspend fun updateUploadStatus(id: Long, status: UploadStatus, mxcUri: String? = null)

    @Query("UPDATE mms_media SET upload_status = :status, failure_reason = :reason WHERE id = :id")
    suspend fun updateUploadFailure(id: Long, status: UploadStatus, reason: String)

    @Query("DELETE FROM mms_media WHERE message_id = :messageId")
    suspend fun deleteForMessage(messageId: Long)
}
