
# Upstream Patches

This document tracks ALL modifications made to AOSP Messaging source code.
Every change must be documented here with rationale.

## Patch Application Order

When updating upstream, apply patches in this order:

1. Build system patches (Android.bp → Gradle)
2. Stub library additions
3. Hidden API removals
4. R.id switch statement conversions
5. Resource conflict resolution
6. Adapter interface wiring

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
- `com/android/ex/photo/Intents.java` (contains PhotoViewIntentBuilder)
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
- `com/android/vcard/VCardEntry.java` (with PhotoData inner class)
- `com/android/vcard/VCardConfig.java`
- `com/android/vcard/VCardParser.java`
- `com/android/vcard/VCardParser_V21.java`
- `com/android/vcard/VCardParser_V30.java`
- `com/android/vcard/VCardInterpreter.java`
- `com/android/vcard/VCardProperty.java`
- `com/android/vcard/VCardEntryCounter.java`
- `com/android/vcard/VCardSourceDetector.java`
- `com/android/vcard/exception/VCardException.java`
- `com/android/vcard/exception/VCardNestedException.java` (extends VCardNotSupportedException)
- `com/android/vcard/exception/VCardNotSupportedException.java`
- `com/android/vcard/exception/VCardVersionException.java`

**Future Work:** Replace with ez-vcard library or copy full AOSP vcard library.

### PATCH-007: R.id Switch Statement Conversion

**Status:** ✅ Complete
**Rationale:** Library modules have non-final R.id values, switch statements don't work
**Files Modified:**
- `com/android/messaging/ui/contact/ContactPickerFragment.java`
- `com/android/messaging/ui/conversationlist/ConversationListActivity.java`
- `com/android/messaging/ui/conversationlist/ArchivedConversationListActivity.java`
- `com/android/messaging/ui/conversationlist/MultiSelectActionModeCallback.java`
- `com/android/messaging/ui/conversation/ConversationFragment.java` (2 methods)
- `com/android/messaging/ui/AttachmentChooserFragment.java`
- `com/android/messaging/ui/VCardDetailFragment.java`
- `com/android/messaging/ui/mediapicker/GalleryGridView.java`
- `com/android/messaging/ui/appsettings/ApplicationSettingsActivity.java`

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

### PATCH-008: Resource Conflict Resolution - iconSize

**Status:** ✅ Complete
**Rationale:** `attr/iconSize` conflicts with Material library's attribute
**Files Modified:**
- `res/values/attrs.xml`: Renamed `iconSize` to `contactIconSize`
- `com/android/messaging/ui/ContactIconView.java`: Updated styleable reference
- All layout files using `app:iconSize` → `app:contactIconSize`

### PATCH-009: Missing Style - PhotoViewTheme.Translucent

**Status:** ✅ Complete
**Rationale:** Parent theme from photo library stub
**Files Modified:**
- `res/values/styles.xml`: Added stub `PhotoViewTheme.Translucent` style

### PATCH-010: Chips Library Attributes

**Status:** ✅ Complete
**Rationale:** RecipientEditTextView uses custom attributes from chips library
**Files Modified:**
- `res/values/attrs.xml`: Added styleable `RecipientEditTextView` with attributes:
  - `unselectedChipTextColor`
  - `unselectedChipBackgroundColor`
  - `avatarPosition`
  - `chipHeight`
  - `imageSpanAlignment`
  - `chipPadding`
  - `chipBackground`
  - `chipTextSize`
  - `chipDeleteIcon`

### PATCH-011: CustomVCardEntry Child Support

**Status:** ✅ Complete
**Rationale:** CustomVCardEntryConstructor uses addChild() method
**Files Modified:**
- `com/android/messaging/datamodel/media/CustomVCardEntry.java`: Added `addChild()` and `getChildren()` methods

### PATCH-012: ContentProvider Authority Prefix Change

**Status:** ✅ Complete
**Rationale:** Avoid conflicts with stock AOSP Messaging app (com.android.messaging)
**Files Modified:**
- `com/android/messaging/datamodel/MessagingContentProvider.java`: Changed AUTHORITY from `com.android.messaging...` to `com.example.messaging...`
- `com/android/messaging/datamodel/MmsFileProvider.java`: Same authority prefix change
- `com/android/messaging/datamodel/MediaScratchFileProvider.java`: Same authority prefix change

**Change Pattern:**
```java
// BEFORE:
public static final String AUTHORITY =
        "com.android.messaging.datamodel.MessagingContentProvider";

// AFTER:
public static final String AUTHORITY =
        "com.example.messaging.datamodel.MessagingContentProvider";
```

**Notes:** This is required to install alongside the stock Messaging app during development/testing. The app manifest must declare matching authorities.

### PATCH-013: Theme Updates for Dark Mode and Edge-to-Edge

**Status:** ✅ Complete
**Rationale:** Support system dark mode and fix content scrolling under status bar
**Files Modified:**
- `res/values/styles.xml`: Changed BugleBaseTheme parent to `Theme.AppCompat.DayNight.DarkActionBar`, added `fitsSystemWindows` attribute
- `res/values/styles.xml`: Updated BugleTheme.ConversationListActivity to use `?android:attr/colorBackground`
- `res/layout/conversation_list_activity.xml`: Added `fitsSystemWindows="true"` to fragment

**Notes:** These changes enable:
- Automatic dark mode following system settings
- Proper handling of system window insets (status bar, navigation bar)

### PATCH-014: Default SMS App Prompt on Startup

**Status:** ✅ Complete
**Rationale:** SMS apps must be the default SMS app to send/receive messages
**Files Modified:**
- `com/android/messaging/ui/conversationlist/ConversationListActivity.java`: Added `promptForDefaultSmsAppIfNeeded()` method called from onCreate

**Change Pattern:**
```java
// Added to onCreate:
promptForDefaultSmsAppIfNeeded();

// New method:
private void promptForDefaultSmsAppIfNeeded() {
    if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, REQUEST_SET_DEFAULT_SMS_APP);
    }
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
- [ ] Apply patches PATCH-007 through PATCH-014 to new source
- [ ] Build and fix new errors
- [ ] Document any NEW patches required
- [ ] Test basic SMS functionality
- [ ] Commit with message: "Update sms-upstream to AOSP android-XX.X.X_rYY"
