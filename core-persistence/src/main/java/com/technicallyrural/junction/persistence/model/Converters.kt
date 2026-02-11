package com.technicallyrural.junction.persistence.model

import androidx.room.TypeConverter
import com.technicallyrural.junction.persistence.entity.ParticipantType
import com.technicallyrural.junction.persistence.entity.UploadStatus

/**
 * Room type converters for enum types.
 */
class Converters {
    @TypeConverter
    fun fromDirection(value: Direction): String = value.name

    @TypeConverter
    fun toDirection(value: String): Direction = Direction.valueOf(value)

    @TypeConverter
    fun fromStatus(value: Status): String = value.name

    @TypeConverter
    fun toStatus(value: String): Status = Status.valueOf(value)

    @TypeConverter
    fun fromParticipantType(value: ParticipantType): String = value.name

    @TypeConverter
    fun toParticipantType(value: String): ParticipantType = ParticipantType.valueOf(value)

    @TypeConverter
    fun fromUploadStatus(value: UploadStatus): String = value.name

    @TypeConverter
    fun toUploadStatus(value: String): UploadStatus = UploadStatus.valueOf(value)
}
