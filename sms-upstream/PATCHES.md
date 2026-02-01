# Upstream Patches

This document tracks ALL modifications made to AOSP Messaging source code.
Every change must be documented here with rationale.

## Patch Application Order

When updating upstream, apply patches in this order:

1. Build system patches (Android.bp → Gradle)
2. Stub library additions
3. Hidden API removals
4. R.id switch statement conversions
5. Adapter interface wiring

---

## Upstream Source Information

**Source:** https://android.googlesource.com/platform/packages/apps/Messaging
**Branch:** main
**Commit:** de315b762312dd1a5d2bbd16e62ef2bd123f61e5
**Date Downloaded:** 2026-02-01

---

## Patch List

### PATCH-001: Build System Conversion

**Status:** ✅ Complete
**Rationale:** Convert from Android.bp/Android.mk to Gradle
**Files Modified:** N/A (build files are new, not patches to upstream)
**Type:** Build configuration only

### PATCH-002: Stub Library - android.support.rastermill

**Status:** ✅ Complete
**Rationale:** FrameSequence library not available as public dependency
**Files Added:**
- `android/support/rastermill/FrameSequence.java`
- `android/support/rastermill/FrameSequenceDrawable.java`

**Future Work:** Replace with Glide or android-gif-drawable for animated images.

### PATCH-003: Stub Library - com.android.ex.chips

**Status:** ✅ Complete
**Rationale:** AOSP chips library not available as public dependency
**Files Added:**
- `com/android/ex/chips/RecipientEntry.java`
- `com/android/ex/chips/RecipientEditTextView.java`
- `com/android/ex/chips/BaseRecipientAdapter.java`
- `com/android/ex/chips/RecipientAlternatesAdapter.java`
- `com/android/ex/chips/PhotoManager.java`
- `com/android/ex/chips/DropdownChipLayouter.java`
- `com/android/ex/chips/recipientchip/DrawableRecipientChip.java`

**Future Work:** Replace with Material Chips or copy full AOSP chips library.

### PATCH-004: Stub Library - com.android.ex.photo

**Status:** ✅ Complete
**Rationale:** AOSP photo viewer library not available as public dependency
**Files Added:**
- `com/android/ex/photo/PhotoViewActivity.java`
- `com/android/ex/photo/PhotoViewCallbacks.java`
- `com/android/ex/photo/PhotoViewController.java`
- `com/android/ex/photo/PhotoViewFragment.java`
- `com/android/ex/photo/Intents/PhotoViewIntentBuilder.java`
- `com/android/ex/photo/adapters/PhotoPagerAdapter.java`
- `com/android/ex/photo/fragments/PhotoViewFragment.java`
- `com/android/ex/photo/loaders/PhotoBitmapLoaderInterface.java`
- `com/android/ex/photo/provider/PhotoContract.java`

**Future Work:** Replace with standard image viewer or copy full AOSP library.

### PATCH-005: Stub Library - com.android.common.contacts

**Status:** ✅ Complete
**Rationale:** DataUsageStatUpdater not available, used for analytics only
**Files Added:**
- `com/android/common/contacts/DataUsageStatUpdater.java`

**Notes:** This is a no-op stub. The class updates contact usage statistics which is non-essential.

### PATCH-006: Stub Library - com.android.vcard

**Status:** ✅ Complete
**Rationale:** AOSP vCard library not available as public dependency
**Files Added:**
- `com/android/vcard/VCardEntry.java`
- `com/android/vcard/VCardConfig.java`
- `com/android/vcard/VCardParser.java`
- `com/android/vcard/VCardParser_V21.java`
- `com/android/vcard/VCardParser_V30.java`
- `com/android/vcard/VCardInterpreter.java`
- `com/android/vcard/VCardProperty.java`
- `com/android/vcard/VCardEntryCounter.java`
- `com/android/vcard/VCardSourceDetector.java`
- `com/android/vcard/exception/VCardException.java`
- `com/android/vcard/exception/VCardNestedException.java`
- `com/android/vcard/exception/VCardNotSupportedException.java`
- `com/android/vcard/exception/VCardVersionException.java`

**Future Work:** Replace with ez-vcard library or copy full AOSP vcard library.

### PATCH-007: Stub Library - androidx.appcompat.mms

**Status:** ⏳ Pending
**Rationale:** AOSP MMS library not available as public dependency
**Files To Add:**
- `androidx/appcompat/mms/ApnSettingsLoader.java`
- `androidx/appcompat/mms/CarrierConfigValuesLoader.java`
- `androidx/appcompat/mms/MmsManager.java`
- `androidx/appcompat/mms/UserAgentInfoLoader.java`
- `androidx/appcompat/mms/pdu/GenericPdu.java`
- `androidx/appcompat/mms/pdu/PduHeaders.java`
- `androidx/appcompat/mms/pdu/PduParser.java`
- `androidx/appcompat/mms/pdu/SendConf.java`

**Future Work:** Copy from AOSP frameworks/opt/mms or create comprehensive stubs.

### PATCH-008: R.id Switch Statement Conversion

**Status:** ⏳ Pending
**Rationale:** Library modules have non-final R.id values, switch statements don't work
**Files To Modify:**
- `com/android/messaging/ui/contact/ContactPickerFragment.java`
- `com/android/messaging/ui/conversation/ConversationFragment.java`
- (Others to be identified)

**Change Pattern:**
```java
// BEFORE:
switch (item.getItemId()) {
    case R.id.action_add:
        // ...
        break;
}

// AFTER:
int id = item.getItemId();
if (id == R.id.action_add) {
    // ...
}
```

---

## Forbidden Changes

The following types of changes are NOT allowed in this module:

- ❌ Matrix-related code
- ❌ GrapheneOS-specific logic
- ❌ UI customizations beyond what's needed for compilation
- ❌ Feature additions
- ❌ New app-specific configuration
- ❌ Theme modifications (beyond fixing compilation)
- ❌ String changes (except removing unused)
- ❌ New dependencies (except essential replacements)

If you need any of the above, it belongs in `app/` module.

---

## Update Checklist

When updating upstream:

- [ ] Download new AOSP source
- [ ] Record commit hash and tag above
- [ ] Replace src/main/java/com/android/messaging with new source
- [ ] Replace src/main/res with new resources
- [ ] Keep stub directories intact (android.support, com.android.ex, etc.)
- [ ] Apply PATCH-007 and PATCH-008 to new source
- [ ] Build and fix new errors
- [ ] Document any NEW patches required
- [ ] Test basic SMS functionality
- [ ] Commit with message: "Update sms-upstream to AOSP android-XX.X.X_rYY"
