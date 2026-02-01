# AOSP Messaging - Standalone Gradle Build

A fork of AOSP Messaging (the Android Open Source Project's default SMS application)
converted to a standalone Gradle-built Android application.

## Project Goals

- **Build in Android Studio** using standard Gradle toolchain
- **Function as default SMS app** on any Android device
- **Run on GrapheneOS** without system privileges
- **Use only public Android SDK APIs** - no hidden APIs, no system signatures
- **Foundation for future Matrix bridge** integration

## Non-Goals

This project is NOT:
- A Matrix integration (yet)
- A UI redesign
- A performance optimization effort
- A Play Store publishing effort

Focus is solely on making AOSP Messaging a clean, standalone Gradle SMS app.

## Target Configuration

| Setting | Value |
|---------|-------|
| minSdk | 29 (Android 10) |
| targetSdk | 34 (Android 14) |
| compileSdk | 34 |
| Gradle | 8.9 |
| AGP | 8.5.2 |
| Java | 17 (source/target compatibility) |

## Why minSdk 29?

Android 10 introduced significant changes to SMS handling and introduced scoped storage.
Supporting older versions would require substantial compatibility shims.

## Required Permissions

All permissions are public SDK permissions:

- `SEND_SMS` - Send SMS messages
- `RECEIVE_SMS` - Receive incoming SMS
- `READ_SMS` - Read SMS from system provider
- `RECEIVE_MMS` - Receive MMS messages
- `RECEIVE_WAP_PUSH` - Receive WAP push (for MMS)
- `READ_CONTACTS` - Display contact names
- `READ_PHONE_STATE` - Access phone number, SIM info
- `READ_PHONE_NUMBERS` - Access phone numbers (API 26+)

## Project Structure

```
AospMessaging/
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── java/          # Java source (from AOSP)
│   │   ├── res/           # Resources (from AOSP)
│   │   ├── aidl/          # AIDL interfaces (if needed)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── docs/                   # Documentation
│   ├── dependency-audit.md
│   └── migration-notes.md
├── aosp-source/           # Original AOSP source (gitignored, reference only)
├── build.gradle.kts       # Project-level build file
├── settings.gradle.kts
└── gradle/
    └── wrapper/
```

## Build Instructions

```bash
./gradlew assembleDebug
```

## Status

- [ ] Phase 0: Ground Rules & Assumptions
- [ ] Phase 1: Source Acquisition & Audit
- [ ] Phase 2: Gradle Scaffolding
- [ ] Phase 3: Code Transplantation
- [ ] Phase 4: Platform Dependency Removal
- [ ] Phase 5: Manifest & Permissions
- [ ] Phase 6: First Build
- [ ] Phase 7: First Install
- [ ] Phase 8: First SMS Send/Receive

## License

Apache 2.0 (same as AOSP)
