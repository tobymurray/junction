# Room Database Migration Guide

**Status:** ✅ KSP Compatibility Resolved (2026-02-10)
**Current:** SharedPreferences
**Recommended:** Room Database for production use

---

## Overview

The KSP + Kotlin 2.3.10 incompatibility has been **resolved**. Room database is now fully supported with the current toolchain:

- ✅ Kotlin 2.3.10
- ✅ KSP 2.3.5
- ✅ Room 2.8.4
- ✅ Verified with test compilation

**Test Result:**
```
> Task :matrix-impl:kspDebugUnitTestKotlin
BUILD SUCCESSFUL in 3s
```

Room annotation processing works correctly with the current setup.

---

## Why Migrate from SharedPreferences to Room?

### Current Limitations (SharedPreferences)

1. **No Type Safety**
   - String keys prone to typos
   - No compile-time validation
   - Manual serialization/deserialization

2. **Poor Performance at Scale**
   - Linear search for queries
   - All data loaded into memory
   - Acceptable for < 100 contacts, degrades beyond that

3. **No Querying Capabilities**
   - Can't filter or sort efficiently
   - Can't do complex queries (e.g., "all rooms created this week")
   - Manual iteration required for searches

4. **No Migration Support**
   - Schema changes require manual handling
   - Risk of data corruption on updates

5. **No Transactions**
   - Race conditions possible with concurrent access
   - Using Mutex as workaround (adds complexity)

### Benefits of Room

1. **Type Safety**
   ```kotlin
   // SharedPreferences
   prefs.getString("phone_to_room_+15550100", null) // Typo risk

   // Room
   dao.getMappingByPhone("+15550100") // Compile-time checked
   ```

2. **Performance**
   - SQLite indexed queries (O(log n) vs O(n))
   - Lazy loading (don't load all data at once)
   - Efficient bulk operations

3. **Query Power**
   ```kotlin
   @Query("SELECT * FROM mappings WHERE roomId LIKE :pattern")
   suspend fun searchRooms(pattern: String): List<RoomMapping>

   @Query("SELECT COUNT(*) FROM mappings")
   suspend fun getMappingCount(): Int
   ```

4. **Automatic Migrations**
   ```kotlin
   @Database(version = 2)
   abstract class AppDatabase : RoomDatabase() {
       companion object {
           val MIGRATION_1_2 = object : Migration(1, 2) {
               override fun migrate(db: SupportSQLiteDatabase) {
                   db.execSQL("ALTER TABLE mappings ADD COLUMN lastUsed INTEGER")
               }
           }
       }
   }
   ```

5. **Built-in Concurrency**
   - Room handles thread safety
   - Suspend functions for coroutines
   - Flow support for reactive updates

---

## Migration Plan

### Phase 1: Define Schema (30 min)

Create `matrix-impl/src/main/java/com/technicallyrural/junction/matrix/impl/db/`:

**RoomMappingEntity.kt:**
```kotlin
package com.technicallyrural.junction.matrix.impl.db

import androidx.room.*

@Entity(
    tableName = "room_mappings",
    indices = [
        Index(value = ["roomId"], unique = true),
        Index(value = ["alias"])
    ]
)
data class RoomMappingEntity(
    @PrimaryKey
    @ColumnInfo(name = "phoneNumber")
    val phoneNumber: String, // E.164 format

    @ColumnInfo(name = "roomId")
    val roomId: String,

    @ColumnInfo(name = "alias")
    val alias: String?,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastUsed")
    val lastUsed: Long = System.currentTimeMillis()
)
```

**RoomMappingDao.kt:**
```kotlin
package com.technicallyrural.junction.matrix.impl.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomMappingDao {
    @Query("SELECT * FROM room_mappings WHERE phoneNumber = :phone")
    suspend fun getMappingByPhone(phone: String): RoomMappingEntity?

    @Query("SELECT * FROM room_mappings WHERE roomId = :roomId")
    suspend fun getMappingByRoomId(roomId: String): RoomMappingEntity?

    @Query("SELECT * FROM room_mappings ORDER BY lastUsed DESC")
    suspend fun getAllMappings(): List<RoomMappingEntity>

    @Query("SELECT * FROM room_mappings ORDER BY lastUsed DESC")
    fun observeAllMappings(): Flow<List<RoomMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: RoomMappingEntity)

    @Update
    suspend fun update(mapping: RoomMappingEntity)

    @Query("UPDATE room_mappings SET lastUsed = :timestamp WHERE phoneNumber = :phone")
    suspend fun updateLastUsed(phone: String, timestamp: Long)

    @Delete
    suspend fun delete(mapping: RoomMappingEntity)

    @Query("DELETE FROM room_mappings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM room_mappings")
    suspend fun count(): Int
}
```

**MatrixDatabase.kt:**
```kotlin
package com.technicallyrural.junction.matrix.impl.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoomMappingEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MatrixDatabase : RoomDatabase() {
    abstract fun roomMappingDao(): RoomMappingDao

    companion object {
        @Volatile
        private var INSTANCE: MatrixDatabase? = null

        fun getDatabase(context: Context): MatrixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MatrixDatabase::class.java,
                    "matrix_bridge.db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### Phase 2: Update Build Config (5 min)

**matrix-impl/build.gradle.kts:**
```kotlin
android {
    // Add this block
    defaultConfig {
        // Schema export location for Room
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}
```

**Add to .gitignore:**
```
matrix-impl/schemas/
```

### Phase 3: Create Room-Based Mapper (45 min)

**RoomBasedRoomMapper.kt:**
```kotlin
package com.technicallyrural.junction.matrix.impl

import android.content.Context
import com.technicallyrural.junction.matrix.*
import com.technicallyrural.junction.matrix.impl.db.*
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId

class RoomBasedRoomMapper(
    context: Context,
    private val clientManager: TrixnityClientManager,
    private val homeserverDomain: String
) : MatrixRoomMapper {

    private val database = MatrixDatabase.getDatabase(context)
    private val dao = database.roomMappingDao()

    override suspend fun getRoomForContact(phoneNumber: String): String? {
        val client = clientManager.client ?: return null

        // 1. Normalize phone number
        val normalized = normalizeToE164(phoneNumber) ?: return null

        // 2. Check database cache
        val cached = dao.getMappingByPhone(normalized)
        if (cached != null) {
            // Update last used timestamp
            dao.updateLastUsed(normalized, System.currentTimeMillis())
            return cached.roomId
        }

        // 3. Try alias resolution
        val alias = buildRoomAlias(normalized)
        val roomByAlias = tryResolveAlias(alias)
        if (roomByAlias != null) {
            dao.insert(
                RoomMappingEntity(
                    phoneNumber = normalized,
                    roomId = roomByAlias,
                    alias = alias
                )
            )
            return roomByAlias
        }

        // 4. Create new room
        return createRoomForContact(normalized, alias)
    }

    override suspend fun getContactForRoom(roomId: String): String? {
        return dao.getMappingByRoomId(roomId)?.phoneNumber
    }

    override suspend fun getAllMappings(): List<RoomMapping> {
        return dao.getAllMappings().map { entity ->
            RoomMapping(
                phoneNumber = entity.phoneNumber,
                roomId = entity.roomId,
                alias = entity.alias
            )
        }
    }

    // Helper methods (same as SimpleRoomMapper)
    private fun normalizeToE164(phone: String): String? { /* ... */ }
    private fun buildRoomAlias(e164: String): String { /* ... */ }
    private suspend fun tryResolveAlias(alias: String): String? { /* ... */ }
    private suspend fun createRoomForContact(phone: String, alias: String): String? { /* ... */ }
}
```

### Phase 4: Data Migration (30 min)

**MigrationHelper.kt:**
```kotlin
package com.technicallyrural.junction.matrix.impl

import android.content.Context
import android.content.SharedPreferences
import com.technicallyrural.junction.matrix.impl.db.*

object MigrationHelper {
    suspend fun migrateFromSharedPreferences(
        context: Context,
        database: MatrixDatabase
    ) {
        val prefs = context.getSharedPreferences(
            "matrix_room_mappings",
            Context.MODE_PRIVATE
        )

        val dao = database.roomMappingDao()

        // Check if migration already done
        if (prefs.getBoolean("migrated_to_room", false)) {
            return
        }

        val mappings = mutableListOf<RoomMappingEntity>()
        val allPrefs = prefs.all

        for ((key, value) in allPrefs) {
            if (key.startsWith("phone_to_room_")) {
                val phone = key.removePrefix("phone_to_room_")
                val roomId = value as? String ?: continue
                val alias = prefs.getString("room_alias_$phone", null)

                mappings.add(
                    RoomMappingEntity(
                        phoneNumber = phone,
                        roomId = roomId,
                        alias = alias
                    )
                )
            }
        }

        // Bulk insert
        mappings.forEach { dao.insert(it) }

        // Mark as migrated
        prefs.edit().putBoolean("migrated_to_room", true).apply()

        println("Migrated ${mappings.size} room mappings from SharedPreferences to Room")
    }
}
```

**Call in MatrixBridgeInitializer:**
```kotlin
class MatrixBridgeInitializer(private val context: Context) {
    suspend fun initialize() {
        // ... existing initialization ...

        // Migrate data on first run with Room
        val database = MatrixDatabase.getDatabase(context)
        MigrationHelper.migrateFromSharedPreferences(context, database)
    }
}
```

### Phase 5: Switch Implementation (10 min)

**MatrixBridgeInitializer.kt:**
```kotlin
// Before
val roomMapper = SimpleRoomMapper(context, clientManager, homeserverDomain)

// After
val roomMapper = RoomBasedRoomMapper(context, clientManager, homeserverDomain)
```

### Phase 6: Testing (1-2 hours)

1. **Unit Tests**
   ```kotlin
   class RoomBasedRoomMapperTest {
       @Test
       fun `insert and retrieve mapping`() = runTest {
           val mapper = createTestMapper()
           val roomId = mapper.getRoomForContact("+15550100")
           assertNotNull(roomId)

           val contact = mapper.getContactForRoom(roomId!!)
           assertEquals("+15550100", contact)
       }
   }
   ```

2. **Integration Tests**
   - Create new room mappings
   - Verify data persists after app restart
   - Test migration from SharedPreferences
   - Verify no data loss

3. **Performance Testing**
   - Create 500+ mappings
   - Measure lookup time (should be < 5ms)
   - Compare with SharedPreferences baseline

---

## Rollback Plan

If issues arise, rollback is simple:

```kotlin
// In MatrixBridgeInitializer.kt
val roomMapper = SimpleRoomMapper(context, clientManager, homeserverDomain) // Revert to this
```

Data remains in SharedPreferences, so no data loss.

---

## Recommended Timeline

| Phase | Time | Priority |
|-------|------|----------|
| Phase 1: Define Schema | 30 min | High |
| Phase 2: Build Config | 5 min | High |
| Phase 3: Room Mapper | 45 min | High |
| Phase 4: Data Migration | 30 min | High |
| Phase 5: Switch | 10 min | High |
| Phase 6: Testing | 1-2 hours | Critical |
| **Total** | **3-4 hours** | |

---

## Current Status Summary

✅ **KSP Compatibility:** Resolved (KSP 2.3.5 + Kotlin 2.3.10)
✅ **Room Dependencies:** Already declared in build.gradle.kts
✅ **Build Verification:** Successful with test Room entity
⏳ **Migration:** Not yet implemented (using SharedPreferences)
📊 **Performance:** SharedPreferences acceptable for < 100 contacts
🎯 **Recommendation:** Migrate to Room for production deployment

---

## References

- [Room Documentation](https://developer.android.com/jetpack/androidx/releases/room)
- [KSP Compatibility](https://kotlinlang.org/docs/ksp-quickstart.html)
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)

---

## Sources

Based on testing and research:
- [KSP quickstart | Kotlin Documentation](https://kotlinlang.org/docs/ksp-quickstart.html)
- [Releases · google/ksp](https://github.com/google/ksp/releases)
- [Room | Jetpack | Android Developers](https://developer.android.com/jetpack/androidx/releases/room)
- [Implementing Room Database in Kotlin Multiplatform + KSP2 + Koin](https://medium.com/@hgarcia.alberto/implementing-room-database-in-kotlin-multiplatform-ksp2-koin-aac564da2d4f)
