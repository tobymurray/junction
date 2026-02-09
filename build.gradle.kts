// Top-level build file
// Module-specific configuration goes in each module's build.gradle.kts

plugins {
// AGP 9.0 handles Kotlin compilation natively, but we apply these aliases
    // so sub-modules can use them without repeating version numbers.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // IMPORTANT: Even though AGP has built-in Kotlin, KSP 2.3+ still
    // requires the Kotlin Gradle Plugin to be present in the buildscript
    // to process annotations correctly.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}

// ============================================================================
// UPSTREAM UPDATE POLICY
// ============================================================================
//
// When AOSP Messaging updates are available:
//
// 1. REPLACE sms-upstream/src/ with new AOSP snapshot
//    - Download new source from android.googlesource.com
//    - Replace files wholesale (do not merge)
//
// 2. RE-APPLY minimal patches
//    - See sms-upstream/PATCHES.md for required modifications
//    - All patches must be documented with rationale
//
// 3. BUILD and FIX at adapter boundaries
//    - Compilation errors should occur in core-sms, not app
//    - Update facades/adapters to match new upstream API
//
// 4. DO NOT modify app/ or Matrix code for upstream changes
//    - If app changes are needed, the adapter layer is wrong
//
// 5. COMMIT with clear message
//    - "Update sms-upstream to AOSP android-14.0.0_r50"
//    - Include AOSP commit hash or tag
//
// ============================================================================
