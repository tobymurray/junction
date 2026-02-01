# Upstream Patches

This document tracks ALL modifications made to AOSP Messaging source code.
Every change must be documented here with rationale.

## Patch Application Order

When updating upstream, apply patches in this order:

1. Build system patches (Android.bp → Gradle)
2. Hidden API removals
3. Internal API replacements
4. Adapter interface wiring

---

## Upstream Source Information

**Source:** https://android.googlesource.com/platform/packages/apps/Messaging
**Branch:** android-14.0.0_rXX (update with actual tag)
**Commit:** (update with actual commit hash)
**Date Downloaded:** (update date)

---

## Patch List

### PATCH-001: Build System Conversion

**Rationale:** Convert from Android.bp/Android.mk to Gradle
**Files Modified:** N/A (build files are new, not patches to upstream)
**Type:** Build configuration only

### PATCH-002: Hidden API Removal - Telephony

**Rationale:** AOSP uses @hide telephony APIs unavailable in SDK
**Files Modified:**
- `src/main/java/com/android/messaging/sms/SmsUtils.java`
- (list all affected files)

**Changes:**
```java
// BEFORE (hidden API):
// TelephonyManager.getDefault().getSubscriptionId()

// AFTER (public API via adapter):
// SmsTransport.getInstance().getDefaultSubscriptionId()
```

### PATCH-003: Hidden API Removal - MMS

**Rationale:** MMS uses internal APIs for PDU handling
**Files Modified:**
- (list files)

**Changes:**
- (document changes)

### PATCH-004: Internal Class Removal

**Rationale:** Remove dependencies on com.android.internal.*
**Files Modified:**
- (list files)

**Changes:**
- (document changes)

### PATCH-005: Adapter Interface Wiring

**Rationale:** Wire upstream code to core-sms facades
**Files Modified:**
- (list files)

**Changes:**
- (document changes)

---

## Forbidden Changes

The following types of changes are NOT allowed in this module:

- ❌ Matrix-related code
- ❌ GrapheneOS-specific logic
- ❌ UI customizations
- ❌ Feature additions
- ❌ Configuration changes
- ❌ Theme modifications
- ❌ String changes (except removing unused)
- ❌ New dependencies (except core-sms)

If you need any of the above, it belongs in `app/` module.

---

## Update Checklist

When updating upstream:

- [ ] Download new AOSP source
- [ ] Record commit hash and tag above
- [ ] Replace src/main/java with new source
- [ ] Replace src/main/res with new resources
- [ ] Apply PATCH-001 through PATCH-XXX
- [ ] Build and fix new errors
- [ ] Document any NEW patches required
- [ ] Test basic SMS functionality
- [ ] Commit with message: "Update sms-upstream to AOSP android-XX.X.X_rYY"
