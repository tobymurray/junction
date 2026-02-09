# Trixnity v4.22.7 API Discovery Guide

This document guides the process of discovering the correct Trixnity v4.22.7 API and implementing real SDK calls.

## Current Status

✅ **Architecture Complete** - All interfaces and classes compile successfully
✅ **Trixnity SDK Enabled** - `trixnity-client:4.22.7` dependency integrated
⏳ **API Calls Stubbed** - Methods marked with TODO, awaiting API verification

## Why API Discovery is Needed

**Problem:** Trixnity v4.22.7 documentation is sparse:
- Official docs show v5.0.0-SNAPSHOT APIs (not yet published)
- v4.22.7 may have different method signatures
- Examples found online may be from different versions

**Approach:** Systematic API discovery to find correct v4.22.7 signatures

---

## API Discovery Methods

### Method 1: Javadoc Inspection (Recommended)

Check the published Javadoc for v4.22.7:

```bash
# Visit in browser
https://javadoc.io/doc/net.folivo/trixnity-client/4.22.7/index.html
```

Look for:
- `MatrixClient` class and its factory methods
- `RoomService` interface and `sendMessage()` signature
- Login methods (may be `loginWithPassword` or `MatrixClient.login`)
- Property names: `userId`, `accessToken`, `deviceId`

### Method 2: Source Code Inspection

Clone the Trixnity repository and checkout v4.22.7:

```bash
git clone https://gitlab.com/connect2x/trixnity/trixnity.git
cd trixnity
git checkout v4.22.7
```

Then examine:
- `trixnity-client/src/commonMain/kotlin/net/folivo/trixnity/client/MatrixClient.kt`
- `trixnity-client/src/commonMain/kotlin/net/folivo/trixnity/client/room/RoomService.kt`
- `trixnity-client/src/commonMain/kotlin/net/folivo/trixnity/client/LoginHelpers.kt`

### Method 3: Dependency Decompilation

Use IntelliJ IDEA or Android Studio to decompile the trixnity-client JAR:

1. Open the project in IntelliJ IDEA / Android Studio
2. Navigate to External Libraries
3. Find `net.folivo:trixnity-client:4.22.7`
4. Expand and browse the decompiled classes
5. Check `net.folivo.trixnity.client.MatrixClient`

### Method 4: Community Resources

Ask in the Trixnity Matrix room:
- Room: `#trixnity:imbitbu.de`
- Ask: "What's the correct API for login and sendMessage in v4.22.7?"

---

## TODOs by Priority

### Priority 1: MatrixClient Creation & Login

**File:** `TrixnityClientManager.kt`

**Current:** Stubbed with mock return values

**Needed:**
1. Find correct login method (`loginWithPassword`, `MatrixClient.login`, or `MatrixClient.create`)
2. Determine parameter types:
   - `baseUrl`: `Url` from `io.ktor.http`?
   - `identifier`: Type for username (possibly `IdentifierType.User` or similar)
   - `repositoriesModule`: How to create (may be `createRepositoriesModule { ... }`)
   - `mediaStore`: Optional or required?
3. Find correct property names on MatrixClient:
   - `userId` or `user.id`?
   - `accessToken` or `token`?
   - `deviceId` or `device.id`?

**Example patterns to verify:**
```kotlin
// Pattern A: loginWithPassword
val client = loginWithPassword(
    baseUrl = Url(serverUrl),
    identifier = IdentifierType.User(username),
    password = password,
    repositoriesModuleFactory = { createRepositoriesModule(...) }
)

// Pattern B: MatrixClient.create with auth
val client = MatrixClient.create(
    repositoriesModule = ...,
    mediaStore = ...,
    auth = MatrixClientAuthProviderData.classicLogin(...)
)

// Pattern C: MatrixClient.login
val client = MatrixClient.login(
    baseUrl = ...,
    identifier = ...,
    password = ...
)
```

### Priority 2: Send Message API

**File:** `TrixnityMatrixBridge.kt`

**Current:** Stubbed with placeholder event ID

**Needed:**
1. Verify `client.room` property exists and type
2. Check `sendMessage` method signature
3. Find correct DSL import for `text()` function
4. Verify return type (likely `Result<EventId>` or `Flow<EventId>`)

**Expected pattern from docs:**
```kotlin
val eventId = client.room.sendMessage(RoomId(roomIdStr)) {
    text("Hello!")
}.getOrThrow()
```

**To verify:**
- Does `client.room` exist or is it `client.roomService()`?
- Is `text()` in `net.folivo.trixnity.client.room.message.text`?
- Does it return `Result<EventId>` or something else?

### Priority 3: Room Creation

**File:** `SimpleRoomMapper.kt`

**Current:** Generates stub room IDs

**Needed:**
1. Find room creation method (likely `client.room.createRoom()`)
2. Verify parameters for:
   - Room name
   - Canonical alias (room alias ID)
   - isDirect flag
   - Preset (trusted private chat)
3. Check return type (likely `Result<RoomId>`)

**Expected pattern:**
```kotlin
val roomId = client.room.createRoom(
    name = "SMS: +15550100",
    roomAliasId = RoomAliasId("#sms_15550100:matrix.org"),
    isDirect = true,
    preset = CreateRoomPreset.TrustedPrivateChat
).getOrThrow()
```

### Priority 4: Room Alias Resolution

**File:** `SimpleRoomMapper.kt`

**Current:** Always returns null

**Needed:**
1. Find API method (likely `client.api.room.getRoomAlias()`)
2. Check parameter type (`RoomAliasId`?)
3. Verify return type (`Result<RoomAliasResolution>` with `roomId` property?)

**Expected pattern:**
```kotlin
val response = client.api.room.getRoomAlias(RoomAliasId(alias)).getOrNull()
val roomId = response?.roomId?.full
```

### Priority 5: Timeline Event Subscription

**File:** `TrixnityMatrixBridge.kt`

**Current:** Empty subscription method

**Needed:**
1. Find timeline event API (may be `client.room.getTimeline()` or `getLastTimelineEvents`)
2. Determine how to filter for:
   - Text messages only
   - Specific rooms
   - Exclude own messages
3. Check event content type extraction

**Expected pattern:**
```kotlin
client.room.getLastTimelineEvents()
    .mapNotNull { timelineEvent ->
        val content = timelineEvent.content as? RoomMessageEventContent.Text
        // ... filter and map
    }
    .collect { message ->
        _inboundMessages.emit(message)
    }
```

### Priority 6: State Event Sending (Presence)

**File:** `TrixnityMatrixBridge.kt`

**Current:** Empty method

**Needed:**
1. Find `sendStateEvent` API
2. Verify parameters (roomId, eventType, stateKey, content)
3. Check content serialization

**Expected pattern:**
```kotlin
client.room.sendStateEvent(
    roomId = RoomId(controlRoomId),
    eventType = "org.technicallyrural.bridge.status",
    stateKey = "device_${deviceId}",
    content = mapOf(...)
)
```

---

## Testing Strategy

Once APIs are implemented:

1. **Unit Test Login**
   ```kotlin
   @Test
   fun `login with valid credentials succeeds`() = runTest {
       val result = clientManager.login(
           serverUrl = "https://matrix.org",
           username = "testuser",
           password = "testpass"
       )
       assertTrue(result is LoginResult.Success)
   }
   ```

2. **Unit Test Message Sending**
   ```kotlin
   @Test
   fun `sendToMatrix creates room and sends message`() = runTest {
       val result = bridge.sendToMatrix(
           phoneNumber = "+15550100",
           messageBody = "Test",
           timestamp = System.currentTimeMillis()
       )
       assertTrue(result is MatrixSendResult.Success)
   }
   ```

3. **Integration Test (Real Server)**
   - Login to test homeserver
   - Create test room
   - Send message
   - Verify message appears in Matrix client

---

## Known Working Example (from matrix-bot-base)

Reference implementation pattern:

```kotlin
private suspend fun getMatrixClient(config: Config): MatrixClient {
    val existingMatrixClient = MatrixClient.create(
        createRepositoriesModule(config),
        createMediaStoreModule(config),
        createCryptoDriverModule()
    ).getOrNull()

    if (existingMatrixClient != null) {
        return existingMatrixClient
    }

    val matrixClient = MatrixClient.create(
        createRepositoriesModule(config),
        createMediaStoreModule(config),
        createCryptoDriverModule(),
        MatrixClientAuthProviderData.classicLogin(
            baseUrl = Url(config.baseUrl),
            identifier = IdentifierType.User(config.username),
            password = config.password,
            initialDeviceDisplayName = "An interesting bot"
        ).getOrThrow()
    ).getOrThrow()

    return matrixClient
}
```

**Note:** This may be from a different Trixnity version. Verify all types exist in v4.22.7.

---

## Resources

- **Official Docs** (may show v5 API): https://trixnity.gitlab.io/trixnity/
- **Javadoc**: https://javadoc.io/doc/net.folivo/trixnity-client/4.22.7/
- **Source Code**: https://gitlab.com/connect2x/trixnity/trixnity (checkout v4.22.7 tag)
- **Matrix Room**: `#trixnity:imbitbu.de`
- **Example Bot**: https://github.com/dfuchss/matrix-bot-base

---

## Success Criteria

✅ All TODO comments replaced with real API calls
✅ Project compiles without errors
✅ Unit tests pass for login and message sending
✅ Integration test: Message successfully sent to Matrix room
✅ No stub/mock return values remaining

---

## Next Steps After API Discovery

1. **Implement Foreground Service** (`MatrixBridgeService`)
   - Keep sync running in background
   - Show persistent notification
   - Handle START_STICKY for restart

2. **Wire SMS ↔ Matrix Flow** (app module)
   - Subscribe to `MatrixBridge.observeMatrixMessages()`
   - Forward to SMS via `SmsTransport.sendSms()`
   - Forward SMS reception to `MatrixBridge.sendToMatrix()`

3. **Implement Control Room**
   - Create `#junction_control:<server>` room
   - Send periodic status updates
   - Include cellular signal, battery, connectivity

4. **End-to-End Testing**
   - Send SMS → verify appears in Matrix
   - Send Matrix message → verify SMS sent
   - Test multiple conversations
   - Test error handling (offline, failed sends)
