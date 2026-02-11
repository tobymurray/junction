# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Junction is a fork of AOSP Messaging (Android's default SMS app) converted to a standalone Gradle-built application with clean architectural separation for upstream updates and Matrix bridge integration.

**Key Goals:**
- Build in Android Studio using standard Gradle toolchain
- Function as default SMS app on any Android device (especially GrapheneOS)
- Use only public Android SDK APIs (no hidden APIs, no system signatures)
- Easy upstream updates through isolated vendored code
- Foundation for Matrix bridge integration (SMS ↔ Matrix bidirectional sync)

## Build Commands

### Standard Development

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device
./gradlew installDebug

# Build release APK (minified, optimized)
./gradlew assembleRelease

# Clean build artifacts
./gradlew clean
```

### Testing

```bash
# Run all unit tests across all modules
./gradlew test

# Run unit tests for specific module
./gradlew :core-sms:test
./gradlew :sms-upstream:test
./gradlew :app:test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew :core-sms:test --tests "*SmsTransportContractTest"
```

### Code Quality

```bash
# Lint all modules
./gradlew lint

# Lint specific module with report
./gradlew :app:lint

# Check for dependency updates (if configured)
./gradlew dependencyUpdates
```

### APK Outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- applicationId: `com.technicallyrural.junction` (+ `.debug` suffix for debug builds)

## Architecture

### Module Dependency Graph

```
app/                     ← Main application (UI, receivers, Matrix orchestration)
  ├─ depends on ─→ core-sms/      ← SMS interface definitions
  ├─ depends on ─→ core-matrix/   ← Matrix bridge interfaces
  ├─ runtime   ─→ sms-upstream/   ← AOSP implementation (via core-sms)
  └─ runtime   ─→ matrix-impl/    ← Trixnity implementation (via core-matrix)

sms-upstream/            ← Vendored AOSP code + adapters
  └─ implements ─→ core-sms/

matrix-impl/             ← Matrix bridge implementation (Trixnity SDK)
  └─ implements ─→ core-matrix/
```

### Strict Layering Rules

**CRITICAL:** The `app/` module must NEVER import directly from:
- `com.android.messaging.*` (sms-upstream internals)
- Matrix implementation classes (matrix-impl internals)

All coupling goes through interface modules (`core-sms/`, `core-matrix/`).

**Why:** Enables mechanical upstream updates and clean separation of concerns. When AOSP updates, only `sms-upstream/` and its adapters change—app code remains untouched.

### Dependency Injection

Both SMS and Matrix dependencies are resolved via singleton registries:
- `CoreSmsRegistry` (in core-sms) — Initialized in `BugleApplication.initializeSync()`
- `MatrixRegistry` (in core-matrix) — Initialized when Matrix login succeeds

Interface implementations are registered at app startup, not passed through constructors.

## Key Design Principles

### 1. Upstream Isolation (sms-upstream/)

AOSP Messaging source is isolated as a black box. The vendored code:
- Contains minimal, documented patches (see `sms-upstream/PATCHES.md`)
- Implements `core-sms/` interfaces via adapter classes
- Has NO app-specific logic, NO Matrix code, NO GrapheneOS adaptations
- Can be wholesale replaced when AOSP updates

**Adapter Location:** `sms-upstream/src/main/java/com/android/messaging/adapter/`

### 2. Interface-Based Coupling (core-sms/, core-matrix/)

All SMS functionality is accessed through stable interfaces:
- `SmsTransport` — Send SMS/MMS messages
- `MessageStore` — Read/write conversations and messages
- `NotificationFacade` — Display message notifications
- `ContactResolver` — Look up contact information
- `SmsReceiveListener` — Receive incoming messages (registered by app)

Matrix bridge follows same pattern with `MatrixBridge`, `MatrixRoomMapper`, `MatrixPresenceService`.

### 3. Manifest Components in App Module

All `BroadcastReceiver`, `Service`, and `Activity` declarations are in `app/AndroidManifest.xml`, not in upstream. This allows:
- Full control over registered components
- Avoiding system-only permissions from upstream manifest
- Dispatching to interfaces instead of AOSP internals

### 4. No Hidden APIs

Only public Android SDK APIs are used. No:
- `@hide` annotations
- `com.android.internal.*` packages
- System signature permissions
- Reflection to access private methods

This ensures compatibility across devices (especially GrapheneOS) and Android versions.

### 5. Edge-to-Edge on Android 15 (targetSdk=35)

AppCompat 1.7+ auto-enables edge-to-edge. The codebase handles this via:
- Custom `WindowInsetsListener` on DecorView (consumes system bars)
- Programmatic top padding on `android.R.id.content` equal to `?attr/actionBarSize`
- Applied in `BugleActionBarActivity.onCreate()` and `.onPostCreate()`

Do NOT set `fitsSystemWindows="true"` in both theme AND layouts—causes double-padding.

## What Goes Where

### In `app/`
- UI (Activities, Fragments, ViewModels, XML layouts)
- BroadcastReceivers (SMS_DELIVER, MMS_RECEIVED, etc.)
- Services (SendService, ReceiveService)
- Matrix bridge orchestration (SMS ↔ Matrix data flow)
- GrapheneOS-specific adaptations
- Permission/role request handling
- App configuration and settings

### In `core-sms/`
- Interface definitions only (SmsTransport, MessageStore, etc.)
- Data classes (Message, Conversation, Participant, etc.)
- Enums (MessageStatus, SendError, MessageType, etc.)
- CoreSmsRegistry singleton for dependency injection
- NO implementations (pure Kotlin interfaces)

### In `sms-upstream/`
- Vendored AOSP Messaging source (455 Java files from commit `de315b76`)
- Adapter implementations (`SmsTransportImpl`, `MessageStoreImpl`, etc.)
- Vendored AOSP libraries:
  - `com.android.ex.chips.*` — Contact chip UI (from klinker41/android-chips)
  - `com.android.ex.photo.*` — Photo viewer (ViewPager-based implementation)
  - `com.android.vcard.*` — vCard parsing (stubs, to be replaced with ez-vcard)
  - `android.support.rastermill.*` — Frame sequence animation (stubs, replace with Glide)
- Minimal documented patches (see PATCHES.md)
- NOTHING app-specific or Matrix-related

### In `core-matrix/`
- Matrix bridge interface definitions only
- `MatrixBridge` — Send SMS to Matrix, observe Matrix messages
- `MatrixRoomMapper` — Map phone numbers to Matrix room IDs (hybrid alias + persistent storage)
- `MatrixPresenceService` — Send device status (signal, data connectivity)
- MatrixRegistry singleton for dependency injection
- NO implementations (pure Kotlin interfaces)

### In `matrix-impl/`
- Trixnity SDK v4.22.7 integration (see `TRIXNITY_INTEGRATION.md`)
- Stub implementations currently compile but need real SDK calls
- Room mapping storage via SharedPreferences (Room database available but not yet implemented)
- Authentication, sync loop, message sending/receiving
- E.164 phone number normalization

## Updating Upstream AOSP Source

When new AOSP versions are released:

1. **Download new source** from android.googlesource.com (specific tag like `android-15.0.0_r1`)
2. **Replace `sms-upstream/src/` wholesale** (don't merge, replace)
3. **Re-apply patches** documented in `sms-upstream/PATCHES.md` in order:
   - Build system patches
   - Stub library additions
   - Hidden API removals
   - R.id switch statement conversions
   - Resource conflict resolution
   - Adapter interface wiring
4. **Fix compilation errors** at adapter boundaries (in `sms-upstream/adapter/`)
5. **Document any new patches** in PATCHES.md with rationale
6. **Commit with clear message** including AOSP commit hash

If app changes are needed during update, the adapter layer is wrong—fix the abstraction.

See `docs/UPSTREAM_UPDATE_GUIDE.md` for detailed step-by-step process.

## Matrix Bridge Integration

### Current Status
- ✅ Architecture complete, all interfaces defined
- ✅ Trixnity SDK 5.1.0 fully integrated
- ✅ Real implementations for login, sync, messaging, and room management
- ℹ️ Using SharedPreferences for room mappings (Room database now available with KSP 2.3.5)

### Trixnity SDK Integration Status
**Version:** 5.1.0 (migrated from 4.22.7 on 2026-02-10)

All core functionality implemented:
1. Initialize MatrixClient from stored credentials or password login
2. Manage sync loop in background service
3. Send SMS messages to Matrix rooms (auto-create DM rooms with aliases)
4. Subscribe to incoming Matrix messages and bridge to SMS
5. Send presence/status updates via custom state events

**Key APIs:**
- `MatrixClient.login()` — Password authentication
- `client.room.sendMessage()` — Send text messages
- `client.room.createRoom()` — Create DM with alias
- `client.getTimelineEventsFromSync()` — Subscribe to messages

**Room Mapping Strategy:** Hybrid alias-preferred with persistent cache
- Aliases: `#sms_+12345678901:homeserver.com` (E.164 normalized)
- Cache: SharedPreferences (phone → roomId, roomId → phone)
- Fallback: Direct room creation if alias unavailable

## Testing Strategy

### Unit Tests
- `core-sms/src/test/` — Contract tests for interface expectations
- `sms-upstream/src/test/` — Adapter implementation tests (verify AOSP adapters work)
- `matrix-impl/src/test/` — Matrix bridge logic tests (phone normalization, mapping)

**Run:** `./gradlew :core-sms:test` or `./gradlew test`

### Integration Tests
- `app/src/androidTest/` — End-to-end UI tests (compose message, send SMS, view conversation)
- Requires connected device or emulator

**Run:** `./gradlew connectedAndroidTest`

### Manual Testing Checklist
1. SMS send/receive ✅
2. MMS send/receive (to be verified)
3. Notifications with correct channel IDs
4. Default SMS app role selection
5. GrapheneOS compatibility
6. Matrix login and sync
7. SMS → Matrix bridge
8. Matrix → SMS bridge

## Common Development Tasks

### Adding a New SMS Interface Method
1. Add method signature to interface in `core-sms/src/main/java/.../`
2. Implement in adapter class in `sms-upstream/src/main/java/.../adapter/`
3. Write contract test in `core-sms/src/test/`
4. Write adapter test in `sms-upstream/src/test/`
5. Use from app via `CoreSmsRegistry.smsTransport.newMethod()`

### Debugging SMS Send Failures
1. Check `SmsTransportImpl.sendTextMessage()` in sms-upstream adapter
2. Verify `SmsManager` permission granted
3. Check `SendService` logs for broadcast receiver callbacks
4. Ensure message stored in `BugleDatabaseHelper` before sending

### Wiring a New Matrix Interface
1. Define interface in `core-matrix/src/main/java/`
2. Add stub implementation in `matrix-impl/src/main/java/.../impl/`
3. Register in `MatrixRegistry` singleton
4. Access from app via `MatrixRegistry.bridge.method()`

## Known Issues & Workarounds

### Gradle Configuration Warnings
AGP 9.0 includes built-in Kotlin support. Current build.gradle.kts files apply `kotlin-android` plugin explicitly for clarity and parcelize support, causing deprecation warnings. This is intentional pending future cleanup.

### Room Database Now Available ✅
~~Room database in `matrix-impl/` cannot be compiled until KSP 2.3.x is released.~~

**UPDATE (2026-02-10):** KSP 2.3.5 is now compatible with Kotlin 2.3.10 and Room 2.8.4. Room database can now be used instead of SharedPreferences.

**Current Implementation:** SharedPreferences for room mapping (acceptable for < 100 contacts, but Room migration is now possible for better performance and reliability).

### ActionBarOverlayLayout + Edge-to-Edge
On API 35, `setDecorFitsSystemWindows(true)` is a no-op. Manual inset handling required in `BugleActionBarActivity`. Do not set `fitsSystemWindows="true"` in both theme and layouts.

### Stub Libraries Still Present
- `android.support.rastermill.*` — Replace with Glide for animated GIFs
- `com.android.vcard.*` — Replace with ez-vcard library
- Stubs compile but provide no functionality

## Version Information

| Component | Version |
|-----------|---------|
| compileSdk | 36 |
| targetSdk | 35 (Android 15) |
| minSdk | 29 (Android 10) |
| AGP | 9.0.0 |
| Gradle | 9.3.1 |
| Kotlin | 2.3.10 |
| Trixnity | 5.1.0 (fully integrated) |
| AOSP Source | commit `de315b76` (2026-02-01) |

## Documentation

- `README.md` — Project overview, build instructions, status checklist
- `docs/ARCHITECTURE.md` — Data flow diagrams, design principles, forbidden patterns
- `docs/UPSTREAM_UPDATE_GUIDE.md` — Step-by-step AOSP update process
- `docs/TESTING_STRATEGY.md` — Test coverage and manual testing procedures
- `sms-upstream/PATCHES.md` — All modifications to vendored AOSP code
- `matrix-impl/TRIXNITY_INTEGRATION.md` — Trixnity SDK integration checklist
- `matrix-impl/TRIXNITY_API_DISCOVERY.md` — API research session log

## Forbidden Patterns

### ❌ Direct Upstream Imports in App
```kotlin
// WRONG - app/ importing from sms-upstream internals
import com.android.messaging.datamodel.BugleDatabaseHelper
```

### ❌ App Logic in Upstream
```java
// WRONG - sms-upstream should not know about Matrix
if (MatrixBridge.isEnabled()) { /* ... */ }
```

### ❌ Skipping Adapter Layer
```kotlin
// WRONG - use SmsTransport interface instead
val smsManager = context.getSystemService(SmsManager::class.java)
smsManager.sendTextMessage(...)
```

### ✅ Correct Pattern
```kotlin
// CORRECT - use core-sms interface via registry
val transport = CoreSmsRegistry.smsTransport
transport.sendSms(destination, message)
```

## Git Workflow

- Main branch: `main`
- Feature branches: `feature/description` or `update-upstream-android-XX`
- Commit messages: Follow conventional commits when practical (e.g., "feat:", "fix:", "docs:")
- After upstream updates: Commit message must include AOSP commit hash

## Code Style

- Java: AOSP style (4-space indent, no star imports)
- Kotlin: Standard Kotlin style (4-space indent, explicit types for public APIs)
- Interfaces: Document all public methods with KDoc/Javadoc
- Adapters: Add comments explaining how upstream AOSP is bridged to interface
