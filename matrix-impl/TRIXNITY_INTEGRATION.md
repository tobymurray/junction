# Trixnity SDK Integration Guide

This document outlines exactly what needs to be implemented to complete the Matrix bridge using Trixnity SDK v4.22.7+ (or v5.x when released).

## Current Status

✅ **Architecture Complete**
- All interfaces defined in `core-matrix/`
- Module structure established
- Room mapping strategy designed (hybrid alias + persistent storage)
- Stub implementations compile and demonstrate data flow

⚠️ **Trixnity Integration Required**
- SDK initialization and authentication
- Room creation and message sending
- Event subscription and timeline processing
- Presence updates via custom state events

---

## Required Trixnity Dependencies

Add to `matrix-impl/build.gradle.kts`:

```kotlin
dependencies {
    // Core Trixnity client
    implementation("net.folivo:trixnity-client:4.22.7")

    // Android-specific repository (Room-based storage)
    implementation("net.folivo:trixnity-client-repository-room:4.22.7")

    // Media store for Android
    implementation("net.folivo:trixnity-client-media-okio:4.22.7")

    // Optional: Encryption support
    // implementation("net.folivo:trixnity-client-crypto-vodozemac:4.22.7")
}
```

---

## 1. MatrixClientManager (`StubMatrixClientManager.kt` → `MatrixClientManager.kt`)

### Current Stub Location
`matrix-impl/src/main/java/com/technicallyrural/junction/matrix/impl/StubMatrixClient.kt`

### Implementation Checklist

#### ✅ Initialize from Stored Credentials

```kotlin
suspend fun initializeFromStore(
    serverUrl: String,
    userId: String,
    deviceId: String,
    accessToken: String
): Boolean {
    // TODO: Replace with actual Trixnity initialization
    val client = MatrixClient.fromStore(
        baseUrl = Url(serverUrl),
        userId = UserId(userId),
        deviceId = DeviceId(deviceId),
        accessToken = accessToken,
        repositoriesModule = createRoomRepositoriesModule(context),
        mediaStore = createMediaStore(context)
    ).getOrNull() ?: return false

    _client = client
    _isInitialized.value = true
    return true
}
```

**Dependencies:**
- `createRoomRepositoriesModule(context)` — Uses Room database for state storage
- `createMediaStore(context)` — File-based media cache

#### ✅ Login with Username/Password

```kotlin
suspend fun login(
    serverUrl: String,
    username: String,
    password: String
): LoginResult {
    // TODO: Replace stub implementation
    val response = MatrixClient.login(
        baseUrl = Url(serverUrl),
        identifier = IdentifierType.User(username),
        password = password,
        deviceDisplayName = "Junction SMS Bridge",
        repositoriesModule = createRoomRepositoriesModule(context),
        mediaStore = createMediaStore(context)
    ).getOrElse {
        return LoginResult.Error(it.message ?: "Login failed")
    }

    return LoginResult.Success(
        userId = response.userId.full,
        accessToken = response.accessToken,
        deviceId = response.deviceId.value
    )
}
```

#### ✅ Sync Loop Management

```kotlin
fun startSync() {
    scope.launch {
        try {
            _isSyncing.value = true
            _client?.startSync() // Blocking call that loops
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
        }
    }
}

fun stopSync() {
    _client?.stopSync()
    _isSyncing.value = false
}
```

---

## 2. MatrixBridge (`StubMatrixBridge` → `MatrixBridgeImpl`)

### Current Stub Location
`matrix-impl/src/main/java/com/technicallyrural/junction/matrix/impl/StubMatrixClient.kt`

### Implementation Checklist

#### ✅ Send SMS Message to Matrix

```kotlin
override suspend fun sendToMatrix(
    phoneNumber: String,
    messageBody: String,
    timestamp: Long,
    isGroup: Boolean
): MatrixSendResult {
    val client = clientManager.client ?: return MatrixSendResult.Failure(MatrixSendError.NOT_CONNECTED)

    // Get or create room for this contact
    val roomIdStr = roomMapper.getRoomForContact(phoneNumber)
        ?: return MatrixSendResult.Failure(MatrixSendError.ROOM_CREATION_FAILED)

    return try {
        // TODO: Use Trixnity API for sending messages
        val eventId = client.room.sendMessage(RoomId(roomIdStr)) {
            text("SMS from $phoneNumber:\n$messageBody")
        }.getOrThrow()

        MatrixSendResult.Success(eventId.toString())
    } catch (e: Exception) {
        e.printStackTrace()
        MatrixSendResult.Failure(MatrixSendError.SEND_FAILED)
    }
}
```

**API Reference:**
- `client.room.sendMessage(roomId) { text(body) }`
- Check Trixnity docs for exact method signature in v4.22.7+

#### ✅ Subscribe to Incoming Messages

```kotlin
private suspend fun subscribeToRoomMessages() {
    val client = clientManager.client ?: return

    scope.launch {
        // TODO: Subscribe to timeline events
        client.room.getTimelineEventsFromSync().collect { event ->
            try {
                // Filter for text messages
                val content = event.content as? RoomMessageEventContent.TextBased.Text
                    ?: return@collect

                // Get phone number for this room
                val phoneNumber = roomMapper.getContactForRoom(event.roomId.full)
                    ?: return@collect

                // Skip our own messages
                if (event.sender == client.userId) return@collect

                // Emit for SMS sending
                _inboundMessages.emit(
                    MatrixInboundMessage(
                        roomId = event.roomId.full,
                        eventId = event.id.toString(),
                        sender = event.sender.full,
                        body = content.body,
                        timestamp = event.originTimestamp.timestamp,
                        messageType = MatrixMessageType.TEXT
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
```

**API Changes (v4.x → v5.x):**
- Event subscription API may have changed
- Check `trixnity.connect2x.de/api/` for latest Flow-based APIs

#### ✅ Send Presence/Status Updates

```kotlin
override suspend fun updatePresence(dataConnected: Boolean, cellSignal: Int) {
    val client = clientManager.client ?: return
    val roomId = controlRoomIdCached ?: return

    try {
        // TODO: Send custom state event
        val content = mapOf(
            "data_connected" to dataConnected,
            "cell_signal" to cellSignal,
            "last_seen" to System.currentTimeMillis(),
            "device_model" to android.os.Build.MODEL,
            "app_version" to "1.0.0"
        )

        client.room.sendStateEvent(
            roomId = RoomId(roomId),
            eventType = "org.technicallyrural.bridge.status",
            stateKey = "device_${client.deviceId}",
            content = content
        ).getOrThrow()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

---

## 3. Room Mapper (`SimpleRoomMapper.kt`)

### Current Status
- Uses SharedPreferences for mapping storage
- Room alias logic designed but stubbed
- E.164 phone normalization implemented ✅

### Implementation Checklist

#### ✅ Resolve Room Alias

```kotlin
private suspend fun tryResolveAlias(client: MatrixClient, alias: String): String? {
    return try {
        // TODO: Use Trixnity alias resolution API
        val response = client.api.room.getRoomAlias(RoomAliasId(alias)).getOrNull()
        response?.roomId?.full
    } catch (e: Exception) {
        null
    }
}
```

#### ✅ Create DM Room with Alias

```kotlin
private suspend fun createRoomForContact(
    client: MatrixClient,
    phoneE164: String,
    alias: String
): String? {
    return try {
        val aliasLocalpart = alias.substringAfter("#").substringBefore(":")

        // TODO: Use Trixnity room creation API
        val roomId = client.room.createRoom(
            name = "SMS: $phoneE164",
            roomAliasLocalpart = aliasLocalpart,
            isDirect = true,
            invite = listOf(client.userId),
            preset = CreateRoomPreset.TRUSTED_PRIVATE_CHAT
        ).getOrThrow()

        saveMapping(phoneE164, roomId.full, alias)
        roomId.full
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

**Un-comment** the commented-out methods in `SimpleRoomMapper.kt` once Trixnity is integrated.

---

## 4. Room Database Migration (Future Enhancement)

### Current: SharedPreferences
- ✅ Works for < 100 contacts
- ✅ Simple, no tooling dependencies
- ❌ No complex queries
- ❌ No foreign key constraints

### Future: Room Database (when KSP supports Kotlin 2.2.10+)

**Preserved Schema** in `matrix-impl/db.TODO/` (removed from build):
- `MatrixRoomMappingEntity.kt` — Entity definition
- `MatrixRoomMappingDao.kt` — DAO queries
- `MatrixDatabase.kt` — Database setup

**Migration Path:**
1. Wait for KSP 2.2.x release
2. Re-enable Room dependencies in `build.gradle.kts`
3. Restore `db/` folder from backup
4. Implement migration from SharedPreferences → Room

---

## 5. Testing the Integration

### Unit Tests

Create tests in `matrix-impl/src/test/`:

```kotlin
class MatrixBridgeImplTest {
    @Test
    fun `sendToMatrix creates room if not exists`() = runTest {
        // Given: Phone number without existing mapping
        // When: sendToMatrix called
        // Then: Room created and message sent
    }

    @Test
    fun `observeMatrixMessages filters self-messages`() = runTest {
        // Verify messages from bridge user are not emitted
    }
}
```

### Integration Tests

1. **Login Test**
   - Try logging into real Matrix homeserver
   - Verify access token stored securely

2. **Room Creation Test**
   - Create room with alias for test phone number
   - Verify alias resolution works

3. **Message Bridge Test**
   - Send SMS → Verify appears in Matrix room
   - Send Matrix message → Verify callback to SMS flow

---

## 6. API Documentation Resources

### Official Trixnity Docs
- API Reference (v5.0-SNAPSHOT): https://trixnity.connect2x.de/api/
- GitHub (mirror): https://github.com/benkuly/trixnity
- GitLab (primary): https://gitlab.com/connect2x/trixnity/trixnity
- Community: Matrix room `#trixnity:imbitbu.de`

### Key Trixnity Modules for Android
- `trixnity-client` — Core client library
- `trixnity-client-repository-room` — Android Room storage backend
- `trixnity-client-media-okio` — Okio-based media storage
- `trixnity-crypto-vodozemac` — Modern encryption (optional)

### Version Notes
- **v4.22.7** — Latest stable (as of 2026-02-09)
- **v5.0.0** — In development (API docs available but artifact not published)
- Use v4.22.7 for production, reference v5.0.0 docs for API patterns

---

## 7. Common Issues & Solutions

### Issue: KSP Incompatibility with Kotlin 2.2.10
**Status:** Blocking Room database migration
**Workaround:** Using SharedPreferences (acceptable for MVP)
**Resolution:** Wait for KSP 2.2.x release or downgrade Kotlin to 2.1.0

### Issue: Trixnity API Changes Between Versions
**Solution:** Always check version-specific docs or ask in Matrix room

### Issue: Media Upload for MMS
**Status:** Stubbed in `sendMmsToMatrix()`
**Implementation:** Use `client.media.upload(file)` then `client.room.sendMessage { image(...) }`

---

## Next Steps

1. **Add Trixnity dependencies** (un-comment in `build.gradle.kts`)
2. **Replace `StubMatrixClientManager`** with real initialization
3. **Implement `subscribeToRoomMessages()`** for Matrix→SMS flow
4. **Test login flow** with real homeserver
5. **Test room creation** and message sending
6. **Implement foreground service** (Task #4) to keep sync alive
7. **Wire SMS↔Matrix orchestration** (Tasks #6-7) in app module

The architecture is complete and compiles. Only Trixnity SDK integration remains!
