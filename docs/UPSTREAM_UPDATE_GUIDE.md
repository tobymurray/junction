# Upstream Update Guide

This document describes the process for updating the AOSP Messaging source
when new Android versions are released.

---

## Overview

The `sms-upstream/` module contains vendored AOSP Messaging source code.
When Google releases new Android versions, this source should be updated
to pick up bug fixes, security patches, and new features.

**Key Principle:** Updates should be MECHANICAL. If updating upstream
requires significant changes to `app/` or Matrix code, the adapter layer
(`core-sms/`) is not providing sufficient isolation.

---

## Update Frequency

- **Security updates:** As soon as possible after AOSP release
- **Feature updates:** Evaluate on a case-by-case basis
- **Major Android versions:** Within 1-2 months of stable release

---

## Pre-Update Checklist

Before starting an update:

- [ ] Current `main` branch builds successfully
- [ ] All tests pass
- [ ] No pending changes in working directory
- [ ] You have the target AOSP tag/commit identified

---

## Step-by-Step Update Process

### Step 1: Identify Target Version

Find the AOSP tag you want to update to:

```bash
# Browse available tags at:
# https://android.googlesource.com/platform/packages/apps/Messaging/+refs

# Common tag patterns:
# android-14.0.0_r50  (Android 14, release 50)
# android-15.0.0_r1   (Android 15, release 1)
```

Record the exact tag: `android-XX.X.X_rYY`

### Step 2: Create Update Branch

```bash
git checkout main
git pull
git checkout -b update-upstream-android-XX
```

### Step 3: Download New Source

```bash
# Remove old source (keep build.gradle.kts, PATCHES.md, AndroidManifest.xml)
rm -rf sms-upstream/src/main/java/*
rm -rf sms-upstream/src/main/res/*
rm -rf sms-upstream/src/main/aidl/*

# Clone fresh AOSP source to temp location
git clone --depth 1 --branch android-XX.X.X_rYY \
    https://android.googlesource.com/platform/packages/apps/Messaging \
    /tmp/aosp-messaging-new

# Copy source files
cp -r /tmp/aosp-messaging-new/src/* sms-upstream/src/main/java/
cp -r /tmp/aosp-messaging-new/res/* sms-upstream/src/main/res/

# Copy AIDL if present
if [ -d /tmp/aosp-messaging-new/src/com/android/messaging/aidl ]; then
    mkdir -p sms-upstream/src/main/aidl
    cp -r /tmp/aosp-messaging-new/src/com/android/messaging/aidl/* \
        sms-upstream/src/main/aidl/
fi

# Clean up
rm -rf /tmp/aosp-messaging-new
```

### Step 4: Update Version Tracking

Edit `sms-upstream/PATCHES.md` and update:

```markdown
## Upstream Source Information

**Source:** https://android.googlesource.com/platform/packages/apps/Messaging
**Branch:** android-XX.X.X_rYY
**Commit:** <full commit hash>
**Date Downloaded:** YYYY-MM-DD
```

### Step 5: Apply Patches

Re-apply all patches documented in `sms-upstream/PATCHES.md`:

1. **Build system patches** - Already done (build.gradle.kts exists)
2. **Hidden API removals** - Apply each patch in order
3. **Internal API replacements** - Apply each patch in order
4. **Adapter interface wiring** - Apply each patch in order

For each patch, follow the documented changes exactly.

### Step 6: Build and Fix Errors

```bash
./gradlew :sms-upstream:compileDebugJavaWithJavac
```

Common errors and fixes:

| Error | Fix |
|-------|-----|
| `cannot find symbol: class X` (internal class) | Remove usage or stub |
| `cannot find symbol: method X` (hidden API) | Replace with public API |
| `package com.android.internal does not exist` | Remove import and usage |
| `incompatible types` (API change) | Update adapter interface |

### Step 7: Update Adapter Layer (if needed)

If upstream API changes break `core-sms/` interfaces:

1. Update the interface in `core-sms/`
2. Update the implementation in `sms-upstream/`
3. Update any usages in `app/`

**Goal:** Changes should be confined to `core-sms/` when possible.

### Step 8: Test

```bash
# Build all modules
./gradlew assembleDebug

# Run tests
./gradlew test

# Install and test on device
./gradlew installDebug
```

Test checklist:
- [ ] App launches
- [ ] Conversations load
- [ ] Send SMS works
- [ ] Receive SMS works
- [ ] Send MMS works (if supported)
- [ ] Notifications work

### Step 9: Document New Patches

If you made ANY changes to `sms-upstream/src/`:

1. Add new patch entry to `sms-upstream/PATCHES.md`
2. Document exact changes with before/after code
3. Explain rationale

### Step 10: Commit and Merge

```bash
git add -A
git commit -m "Update sms-upstream to AOSP android-XX.X.X_rYY

- Updated source from android.googlesource.com
- Applied patches from PATCHES.md
- [List any new patches added]

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"

git checkout main
git merge update-upstream-android-XX
```

---

## Troubleshooting

### "I need to change app/ code for this update"

This suggests the adapter layer is leaking upstream details.

**Solution:**
1. Identify what upstream detail is leaking
2. Add abstraction to `core-sms/` interface
3. Implement adapter in `sms-upstream/`
4. Update `app/` to use new interface

### "Hidden API was removed in new Android version"

The hidden API we were stubbing no longer exists.

**Solution:**
1. Remove the stub
2. If feature depended on it, find alternative or remove feature
3. Document in PATCHES.md

### "New feature requires hidden API"

We cannot use hidden APIs.

**Solution:**
1. Do not implement the feature
2. Document the limitation
3. File Android issue requesting public API if appropriate

### "Merge conflict in patches"

Upstream changed code we patched.

**Solution:**
1. Understand the upstream change
2. Reapply patch to new code structure
3. Update PATCHES.md with new change

---

## What MUST NOT Change During Updates

These files should NOT be modified during upstream updates:

- `app/` module (all files)
- `core-sms/` interfaces (unless upstream API changes require it)
- `build.gradle.kts` files (unless dependency versions need updating)
- `settings.gradle.kts`
- `gradle/libs.versions.toml` (unless updating dependencies)

---

## What CAN Change During Updates

- `sms-upstream/src/` (wholesale replacement + patches)
- `sms-upstream/PATCHES.md` (must be updated)
- `core-sms/` implementations (to match new upstream)

---

## Emergency Rollback

If an update breaks things badly:

```bash
git checkout main
git revert HEAD  # If already merged
# or
git branch -D update-upstream-android-XX  # If not merged
```

---

## Updating Vendored Libraries

In addition to the main AOSP Messaging source, the project vendors several AOSP libraries that aren't available as public dependencies.

### Chips Library (com.android.ex.chips)

**Current Source:** https://github.com/klinker41/android-chips
**Location:** `sms-upstream/src/main/java/com/android/ex/chips/`
**Documentation:** `sms-upstream/src/main/java/com/android/ex/chips/README.md`

The chips library provides RecipientEditTextView for contact chip UI components.

**Update Process:**

1. Clone the latest version:
   ```bash
   git clone https://github.com/klinker41/android-chips /tmp/android-chips
   cd /tmp/android-chips
   # Record the commit hash
   git log -1 --format="%H"
   ```

2. Replace the chips source:
   ```bash
   cd /home/toby/AndroidStudioProjects/AospMessaging
   rm -rf sms-upstream/src/main/java/com/android/ex/chips
   cp -r /tmp/android-chips/library/src/main/java/com/android/ex/chips \
       sms-upstream/src/main/java/com/android/ex/
   ```

3. Re-apply build compatibility changes (documented in chips/README.md):
   - Add `import com.android.messaging.R;` to files referencing R
   - Change `android.support.annotation.*` to `androidx.annotation.*`
   - Adjust visibility modifiers (private → protected where needed)
   - Add compatibility methods (`appendRecipientEntry`, `removeRecipientEntry`)
   - Add custom attributes to attrs.xml

4. Update resources:
   - Copy new drawables, layouts, values from `/tmp/android-chips/library/src/main/res/`
   - Preserve Messaging-specific customizations (e.g., `chips_alternates_dropdown_item.xml`)

5. Build and test:
   ```bash
   ./gradlew :sms-upstream:compileDebugJava
   ./gradlew :app:assembleDebug
   ```

6. Update documentation:
   - Update commit hash and date in `chips/README.md`
   - Document any new modifications required

**Note:** The chips library changes infrequently. Only update if there are security fixes or needed features.

---

## Version History

| Date | Version | Notes |
|------|---------|-------|
| 2026-02-01 | android-main (de315b7) | Initial vendoring |
| 2026-02-08 | Chips library vendored | Replaced stubs with full AOSP chips library |

