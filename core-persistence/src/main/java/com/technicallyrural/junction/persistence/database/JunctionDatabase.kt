package com.technicallyrural.junction.persistence.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.technicallyrural.junction.persistence.dao.BridgedMessageDao
import com.technicallyrural.junction.persistence.dao.MessageParticipantDao
import com.technicallyrural.junction.persistence.dao.RoomMappingDao
import com.technicallyrural.junction.persistence.dao.MmsMediaDao
import com.technicallyrural.junction.persistence.entity.BridgedMessageEntity
import com.technicallyrural.junction.persistence.entity.MessageParticipantEntity
import com.technicallyrural.junction.persistence.entity.RoomMappingEntity
import com.technicallyrural.junction.persistence.entity.MmsMediaEntity
import com.technicallyrural.junction.persistence.model.Converters

/**
 * Junction Room database.
 *
 * Version 1: Initial schema with conversation support
 * - BridgedMessageEntity: Message mapping with conversation context
 * - MessageParticipantEntity: Multi-participant support for group messages
 * - RoomMappingEntity: Conversation ↔ Matrix room mapping
 * - MmsMediaEntity: MMS media tracking
 */
@Database(
    entities = [
        BridgedMessageEntity::class,
        MessageParticipantEntity::class,
        RoomMappingEntity::class,
        MmsMediaEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class JunctionDatabase : RoomDatabase() {

    abstract fun bridgedMessageDao(): BridgedMessageDao
    abstract fun messageParticipantDao(): MessageParticipantDao
    abstract fun roomMappingDao(): RoomMappingDao
    abstract fun mmsMediaDao(): MmsMediaDao

    companion object {
        private const val DATABASE_NAME = "junction.db"

        @Volatile
        private var INSTANCE: JunctionDatabase? = null

        /**
         * Get singleton database instance.
         */
        fun getInstance(context: Context): JunctionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JunctionDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Reset instance (for testing).
         */
        @androidx.annotation.VisibleForTesting
        fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
