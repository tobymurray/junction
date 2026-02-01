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
| targetSdk | 34 (Android 14) |
| compileSdk | 34 |
| Gradle | 8.9 |
| AGP | 8.5.2 |
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
- [x] Core interfaces defined
- [x] App manifest with SMS app declarations
- [ ] AOSP source vendored
- [ ] Hidden API patches applied
- [ ] First successful build
- [ ] First successful install
- [ ] SMS send/receive working

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - Design principles and data flow
- [UPSTREAM_UPDATE_GUIDE.md](docs/UPSTREAM_UPDATE_GUIDE.md) - How to update AOSP source
- [SESSION_LOG.md](docs/SESSION_LOG.md) - Multi-session progress tracking
- [PATCHES.md](sms-upstream/PATCHES.md) - Upstream modifications

## License

Apache 2.0 (same as AOSP)
