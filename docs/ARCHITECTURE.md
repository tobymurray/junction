# Architecture Overview

## Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                           app/                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │     UI      │  │   Matrix    │  │  Receivers & Services   │  │
│  │  Activities │  │   Bridge    │  │  (Manifest components)  │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
│         │                │                      │                │
│         └────────────────┼──────────────────────┘                │
│                          │                                       │
│                          ▼                                       │
│              ┌───────────────────────┐                          │
│              │   Depends ONLY on     │                          │
│              │   core-sms interfaces │                          │
│              └───────────────────────┘                          │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              │ implementation(project(":core-sms"))
                              │ implementation(project(":sms-upstream"))
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        core-sms/                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    INTERFACES ONLY                       │    │
│  │                                                          │    │
│  │  SmsTransport     MessageStore     NotificationFacade   │    │
│  │  ContactResolver  SmsReceiveListener                    │    │
│  │                                                          │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  This module has NO dependencies on sms-upstream.               │
│  It defines the contract that sms-upstream implements.          │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              │ implementation(project(":core-sms"))
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       sms-upstream/                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              VENDORED AOSP MESSAGING SOURCE              │    │
│  │                                                          │    │
│  │  - Minimal patches (documented in PATCHES.md)           │    │
│  │  - Implements core-sms interfaces                        │    │
│  │  - NO Matrix code, NO app-specific logic                │    │
│  │  - Replaceable with new AOSP versions                   │    │
│  │                                                          │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Key Design Principles

### 1. Upstream Isolation

The `sms-upstream/` module is a **black box** from the app's perspective.
The app never imports classes from `com.android.messaging.*` directly.
All interaction goes through `core-sms/` interfaces.

**Why:** When AOSP updates, only `sms-upstream/` and `core-sms/` adapters
need to change. The app remains untouched.

### 2. Interface-Based Coupling

All communication between `app/` and SMS functionality uses interfaces:

| Interface | Purpose |
|-----------|---------|
| `SmsTransport` | Send SMS/MMS messages |
| `MessageStore` | Read/write conversations and messages |
| `NotificationFacade` | Display message notifications |
| `ContactResolver` | Look up contact information |
| `SmsReceiveListener` | Receive incoming messages |

**Why:** Interfaces provide stable contracts. Implementations can change
without affecting callers.

### 3. Manifest Components in App Module

All `BroadcastReceiver`, `Service`, and `Activity` declarations are in
the `app/` module's `AndroidManifest.xml`, not in `sms-upstream/`.

**Why:**
- We control what components are registered
- Upstream manifest contains system-only permissions we can't use
- BroadcastReceivers must dispatch to our interfaces, not upstream code

### 4. No Hidden APIs

The project uses ONLY public Android SDK APIs. No:
- `@hide` annotations
- `com.android.internal.*` packages
- System signature permissions
- Reflection to access private methods

**Why:** Hidden APIs can change without notice, break on different devices,
and are blocked on newer Android versions.

## Data Flow Examples

### Sending an SMS

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User taps Send in app/                                       │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. App calls SmsTransport.sendSms()                             │
│    (interface from core-sms/)                                   │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. SmsTransportImpl (in sms-upstream/) handles the call         │
│    - Uses android.telephony.SmsManager (public API)             │
│    - Stores message via MessageStore                            │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. System sends message, calls back with result                 │
│    - SmsTransportImpl updates message status                    │
│    - Returns result through SmsCallback                         │
└─────────────────────────────────────────────────────────────────┘
```

### Receiving an SMS

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. System broadcasts SMS_DELIVER intent                         │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. SmsDeliverReceiver (in app/) receives broadcast              │
│    - Registered in app/AndroidManifest.xml                      │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Receiver extracts SMS data, calls:                           │
│    SmsReceiverRegistry.getListener()?.onSmsReceived(data)       │
│    (interface from core-sms/)                                   │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. SmsReceiveListenerImpl (in sms-upstream/) handles message    │
│    - Stores in MessageStore                                     │
│    - Notifies via NotificationFacade                            │
│    - Updates conversation                                       │
└─────────────────────────────────────────────────────────────────┘
```

## What Goes Where

### In `app/`

- UI (Activities, Fragments, ViewModels)
- BroadcastReceivers (registered in manifest)
- Services (registered in manifest)
- Matrix bridge integration (future)
- GrapheneOS-specific adaptations
- Permission request handling
- Role request handling (default SMS app)
- App configuration and settings

### In `core-sms/`

- Interface definitions (SmsTransport, MessageStore, etc.)
- Data classes (Message, Conversation, etc.)
- Enums (MessageStatus, SendError, etc.)
- Listener registries (SmsReceiverRegistry)
- NO implementations

### In `sms-upstream/`

- AOSP Messaging source code (vendored)
- Implementations of core-sms interfaces
- Minimal patches to remove hidden API usage
- NOTHING app-specific

## Forbidden Patterns

### ❌ App importing upstream classes

```kotlin
// WRONG - app/ should never import from sms-upstream internals
import com.android.messaging.datamodel.BugleDatabaseHelper
```

### ❌ Upstream containing app logic

```java
// WRONG - sms-upstream should not know about Matrix
if (MatrixBridge.isEnabled()) {
    // Matrix-specific behavior
}
```

### ❌ Skipping the adapter layer

```kotlin
// WRONG - even if SmsManager is public, go through SmsTransport
val smsManager = context.getSystemService(SmsManager::class.java)
smsManager.sendTextMessage(...)
```

### ✅ Correct pattern

```kotlin
// CORRECT - use core-sms interface
val transport: SmsTransport = // injected or obtained from registry
transport.sendSms(destination, message)
```

## Future Extensions

### Matrix Bridge

When adding Matrix integration:

1. Add Matrix-specific interfaces to `core-sms/` if needed
2. Create Matrix implementation in `app/` module
3. Bridge between SMS interfaces and Matrix SDK
4. NEVER modify `sms-upstream/` for Matrix support

### Additional Messaging Protocols

The architecture supports adding more transports:

- RCS (Rich Communication Services)
- Signal Protocol
- Other encrypted messaging

Each would be a separate implementation of the `SmsTransport`-like
interfaces, selected at runtime based on availability and user preference.
