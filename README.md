# AOSP Messaging - Standalone Gradle Build

A fork of AOSP Messaging (the Android Open Source Project's default SMS application)
converted to a standalone Gradle-built Android application with clean architectural
separation for easy upstream updates.

## Architecture

```
AospMessaging/
├── sms-upstream/     # Vendored AOSP Messaging (minimal patches)
├── core-sms/         # Adapter interfaces (SmsTransport, MessageStore, etc.)
├── app/              # Main application (UI, Matrix bridge, receivers)
└── docs/             # Architecture and update documentation
```

**Key Principle:** Upstream AOSP code is isolated in `sms-upstream/` as a library
module. The `app/` module NEVER imports directly from upstream internals. All
coupling goes through `core-sms/` interfaces.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed design.

## Project Goals

- **Build in Android Studio** using standard Gradle toolchain
- **Function as default SMS app** on any Android device
- **Run on GrapheneOS** without system privileges
- **Use only public Android SDK APIs** - no hidden APIs, no system signatures
- **Easy upstream updates** - mechanical process with minimal merge pain
- **Foundation for future Matrix bridge** integration

## Non-Goals

This project is NOT:
- A Matrix integration (yet)
- A UI redesign
- A performance optimization effort
- A Play Store publishing effort

## Target Configuration

| Setting | Value |
|---------|-------|
| minSdk | 29 (Android 10) |
| targetSdk | 35 (Android 15) |
| compileSdk | 36 |
| Gradle | 9.3.1 |
| AGP | 9.0.0 |
| Kotlin | 2.3.0 (built-in via AGP) |
| Java | 17 (source/target compatibility) |

## Module Overview

### `sms-upstream/` - Vendored AOSP Code

Contains AOSP Messaging source with minimal patches to:
- Remove hidden API usage
- Remove internal class dependencies
- Wire to core-sms interfaces

**Rules:**
- NO Matrix code
- NO GrapheneOS-specific code
- NO app-specific configuration
- ALL changes documented in [sms-upstream/PATCHES.md](sms-upstream/PATCHES.md)

### `core-sms/` - Adapter Layer

Defines interfaces that abstract SMS functionality:
- `SmsTransport` - Send SMS/MMS
- `MessageStore` - Conversation/message storage
- `NotificationFacade` - Message notifications
- `ContactResolver` - Contact lookup
- `SmsReceiveListener` - Incoming message handling

**Rules:**
- Interfaces only, no implementations
- No dependency on sms-upstream
- Stable API that rarely changes

### `app/` - Main Application

The actual Android application containing:
- UI (Activities, Fragments)
- BroadcastReceivers for SMS/MMS
- Services for headless sending
- Role/permission handling
- Future: Matrix bridge

**Rules:**
- Depends only on core-sms interfaces
- Never imports from com.android.messaging.* directly

## Build Instructions

```bash
# Clone the repository
git clone <repo-url>
cd AospMessaging

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## Updating Upstream

When new AOSP versions are released:

1. Download new source from android.googlesource.com
2. Replace `sms-upstream/src/` wholesale
3. Re-apply patches from `PATCHES.md`
4. Fix compilation errors at adapter boundaries
5. Document any new patches required

See [docs/UPSTREAM_UPDATE_GUIDE.md](docs/UPSTREAM_UPDATE_GUIDE.md) for detailed steps.

## Required Permissions

All permissions are public SDK permissions:

| Permission | Purpose |
|------------|---------|
| `SEND_SMS` | Send SMS messages |
| `RECEIVE_SMS` | Receive incoming SMS |
| `READ_SMS` | Read SMS from system provider |
| `RECEIVE_MMS` | Receive MMS messages |
| `RECEIVE_WAP_PUSH` | Receive WAP push (for MMS) |
| `READ_CONTACTS` | Display contact names |
| `READ_PHONE_STATE` | Access phone number, SIM info |

## Status

- [x] Project structure created
- [x] Gradle multi-module setup
- [x] Core interfaces defined (5 Kotlin interfaces in `core-sms/`)
- [x] App manifest with SMS app declarations
- [x] AOSP source vendored (commit de315b76, 501 Java files)
- [x] Hidden API patches applied (14 patches documented in PATCHES.md)
- [x] First successful build (24MB debug APK)
- [x] First successful install
- [x] SMS send/receive working
- [ ] MMS send/receive verified
- [ ] Core-sms interface implementations wired
- [ ] Unit test coverage
- [ ] Integration test coverage
- [ ] GrapheneOS compatibility verified
- [ ] Matrix bridge foundation

## Status Evaluation & Plan

*Last evaluated: 2026-02-03*

### Current Accuracy Assessment

| Original Status Item | Assessment | Rationale |
|---------------------|------------|-----------|
| Project structure created | **Accurate** | All 4 modules exist: `sms-upstream/`, `core-sms/`, `app/`, `docs/` |
| Gradle multi-module setup | **Accurate** | Three Android modules with `build.gradle.kts`, version catalog in `gradle/libs.versions.toml` |
| Core interfaces defined | **Accurate** | 5 Kotlin interfaces exist: `SmsTransport`, `MessageStore`, `NotificationFacade`, `ContactResolver`, `SmsReceiver` |
| App manifest with SMS app declarations | **Accurate** | 500-line manifest with 11 receivers, 4 services, 13 activities, 3 content providers |
| AOSP source vendored | **Inaccurate (was unchecked)** | Completed: 501 Java files from AOSP commit de315b76, documented in PATCHES.md |
| Hidden API patches applied | **Inaccurate (was unchecked)** | Completed: 14 patches applied including 32 stub library files for rastermill, chips, photo, contacts, vcard |
| First successful build | **Inaccurate (was unchecked)** | Completed: `app/build/outputs/apk/debug/app-debug.apk` exists (24MB) |
| First successful install | **Inaccurate (was unchecked)** | Completed: Git commit "Get SMS working" (d058257) implies successful install and testing |
| SMS send/receive working | **Inaccurate (was unchecked)** | Completed: Git commit message explicitly states "Get SMS working" |

### Verified Current State

Based on codebase inspection (2026-02-03):

- **Build System:** Modernized to AGP 9.0.0, Gradle 9.3.1, Kotlin 2.3.0, compileSdk 36
- **AOSP Source:** 455 files in `com.android.messaging` package, fully vendored
- **Stub Libraries:** 32 files across 5 packages (rastermill, chips, photo, contacts, vcard)
- **Patches:** 14 documented in `sms-upstream/PATCHES.md` with clear rationale and file lists
- **App Package:** `com.technicallyrural.junction` (renamed from `com.example.messaging`)
- **Content Providers:** Authorities prefixed with `com.technicallyrural.junction.datamodel.*`
- **APK:** Debug build exists and is recent (Feb 3, 2026)
- **Core Interfaces:** Defined but no implementations wired between modules

### Remaining Work (Actionable)

#### Phase 1: Interface Implementation & Wiring

- [ ] **Implement `SmsTransport` adapter** (Scope: core-sms, sms-upstream)
  - Completion: Unit test demonstrates SMS can be sent via interface
- [ ] **Implement `MessageStore` adapter** (Scope: core-sms, sms-upstream)
  - Completion: Unit test demonstrates message CRUD via interface
- [ ] **Implement `ContactResolver` adapter** (Scope: core-sms, sms-upstream)
  - Completion: Unit test demonstrates contact lookup via interface
- [ ] **Implement `NotificationFacade` adapter** (Scope: core-sms, sms-upstream)
  - Completion: Unit test demonstrates notification creation via interface
- [ ] **Implement `SmsReceiver` adapter** (Scope: core-sms, sms-upstream)
  - Completion: Unit test demonstrates receive callback via interface
- [ ] **Wire app module to use core-sms interfaces** (Scope: app)
  - Completion: App module has zero direct imports from `com.android.messaging.*`

#### Phase 2: MMS Verification

- [ ] **Test MMS send functionality** (Scope: Manual testing)
  - Completion: Screenshot/log evidence of MMS sent successfully
- [ ] **Test MMS receive functionality** (Scope: Manual testing)
  - Completion: Screenshot/log evidence of MMS received with attachments
- [ ] **Document MMS limitations** (Scope: docs)
  - Completion: Known issues documented if any exist

#### Phase 3: Testing Infrastructure

- [ ] **Add unit tests for core-sms interfaces** (Scope: core-sms/src/test)
  - Completion: `./gradlew :core-sms:test` passes with >80% coverage
- [ ] **Add integration tests for sms-upstream adapters** (Scope: sms-upstream/src/androidTest)
  - Completion: `./gradlew :sms-upstream:connectedAndroidTest` passes
- [ ] **Add UI tests for critical flows** (Scope: app/src/androidTest)
  - Completion: Compose message, send SMS, view conversation tests pass

#### Phase 4: GrapheneOS Compatibility

- [ ] **Test on GrapheneOS device** (Scope: Manual testing)
  - Completion: App installs and functions as default SMS app
- [ ] **Verify no hidden API usage** (Scope: Build verification)
  - Completion: Build with `strictApi` check passes
- [ ] **Document GrapheneOS-specific behavior** (Scope: docs)
  - Completion: Any compatibility notes added to README

#### Phase 5: Polish & Documentation

- [ ] **Replace stub libraries with real implementations** (Scope: sms-upstream)
  - rastermill → Glide/android-gif-drawable
  - chips → Material Chips or full AOSP chips
  - photo → Standard image viewer
  - vcard → ez-vcard library
  - Completion: Stub directories removed, real libraries integrated
- [ ] **Update SESSION_LOG.md with current state** (Scope: docs)
  - Completion: Session log matches actual codebase state
- [ ] **Add contribution guidelines** (Scope: docs)
  - Completion: CONTRIBUTING.md exists with workflow

### Execution Plan for Future Sessions

Each step is independently completable and ends with a verifiable outcome.

| Step | Task | Files/Modules | Verification | Dependencies |
|------|------|---------------|--------------|--------------|
| 1 | Create `SmsTransportImpl` adapter class | `sms-upstream/src/main/java/.../adapter/SmsTransportImpl.kt` | Class compiles, implements `SmsTransport` | None |
| 2 | Create `MessageStoreImpl` adapter class | `sms-upstream/src/main/java/.../adapter/MessageStoreImpl.kt` | Class compiles, implements `MessageStore` | None |
| 3 | Create `ContactResolverImpl` adapter class | `sms-upstream/src/main/java/.../adapter/ContactResolverImpl.kt` | Class compiles, implements `ContactResolver` | None |
| 4 | Create `NotificationFacadeImpl` adapter class | `sms-upstream/src/main/java/.../adapter/NotificationFacadeImpl.kt` | Class compiles, implements `NotificationFacade` | None |
| 5 | Create `SmsReceiverImpl` adapter class | `sms-upstream/src/main/java/.../adapter/SmsReceiverImpl.kt` | Class compiles, implements `SmsReceiver` | None |
| 6 | Add dependency injection setup | `app/build.gradle.kts`, `app/src/main/java/.../di/` | Interfaces can be injected into app components | Steps 1-5 |
| 7 | Refactor app module to use interfaces | `app/src/main/java/` (receivers, services) | No direct `com.android.messaging.*` imports in app module | Step 6 |
| 8 | Add unit test infrastructure | `core-sms/build.gradle.kts`, `core-sms/src/test/` | `./gradlew :core-sms:test` runs | None |
| 9 | Write interface contract tests | `core-sms/src/test/java/.../` | Tests define expected behavior | Step 8 |
| 10 | Write adapter implementation tests | `sms-upstream/src/test/java/.../` | Adapter tests pass | Steps 1-5, 8 |
| 11 | Manual MMS testing | N/A | Document results in `docs/TESTING.md` | None |
| 12 | GrapheneOS device testing | N/A | Document results in `docs/TESTING.md` | None |
| 13 | Replace rastermill stub with Glide | `sms-upstream/build.gradle.kts`, delete stub files | Animated GIFs display correctly | None |
| 14 | Replace chips stub with Material Chips | `sms-upstream/build.gradle.kts`, UI files | Recipient selection works | None |
| 15 | Update SESSION_LOG.md | `docs/SESSION_LOG.md` | Log reflects completed work | After any step |

**Session Start Checklist:**
1. Read this section of README.md
2. Run `git status` and `git log -5 --oneline`
3. Run `./gradlew assembleDebug` to verify build still works
4. Pick next uncompleted step from table above
5. After completion, update Status checklist and this plan

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - Design principles and data flow
- [UPSTREAM_UPDATE_GUIDE.md](docs/UPSTREAM_UPDATE_GUIDE.md) - How to update AOSP source
- [SESSION_LOG.md](docs/SESSION_LOG.md) - Multi-session progress tracking
- [PATCHES.md](sms-upstream/PATCHES.md) - Upstream modifications

## License

Apache 2.0 (same as AOSP)
