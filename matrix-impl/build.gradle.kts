// ============================================================================
// MATRIX-IMPL MODULE - Matrix Bridge Implementation
// ============================================================================
//
// This module contains:
// - Trixnity SDK integration
// - Implementations of core-matrix interfaces
// - Room database for room mapping
// - Matrix client manager and lifecycle
//
// DEPENDENCY RULES:
// - Implements core-matrix interfaces
// - Uses app module's MatrixConfigRepository via dependency injection
// - NO direct UI code (that's in app module)
// - NO SMS code (use core-sms interfaces only if needed)
//
// ============================================================================

plugins {
    alias(libs.plugins.android.library)
    // AGP 9.0 provides built-in Kotlin support
    // TODO: Add KSP when compatible version is available for Kotlin 2.2.10
}

android {
    namespace = "com.technicallyrural.junction.matrix.impl"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    // ========================================================================
    // Module dependencies
    // ========================================================================

    // Core Matrix interfaces
    implementation(project(":core-matrix"))

    // ========================================================================
    // Matrix SDK - Trixnity
    // TODO: Enable when API usage is clarified for v4.22.7+
    // For now, using stub implementations
    // ========================================================================
    // implementation(libs.trixnity.client)

    // ========================================================================
    // Room database for room mapping
    // TODO: Enable when KSP is compatible with Kotlin 2.2.10
    // Stub implementation uses SharedPreferences
    // ========================================================================
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)

    // ========================================================================
    // AndroidX
    // ========================================================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ========================================================================
    // Testing
    // ========================================================================
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
}
