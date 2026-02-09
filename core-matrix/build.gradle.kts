// ============================================================================
// CORE-MATRIX MODULE - Matrix Bridge Interfaces
// ============================================================================
//
// This module contains:
// - Interface definitions for Matrix bridge functionality
// - Data classes for Matrix messages and room mappings
// - MatrixRegistry for dependency injection
// - NO implementations (provided by matrix-impl module)
//
// DEPENDENCY RULES:
// - Can depend on Kotlin stdlib and coroutines only
// - NO Android dependencies (pure Kotlin library)
// - NO Matrix SDK dependencies (that's in matrix-impl)
//
// ============================================================================

plugins {
    alias(libs.plugins.android.library)
    // AGP 9.0 provides built-in Kotlin support
}

android {
    namespace = "com.technicallyrural.junction.matrix"
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
    // Core dependencies
    // ========================================================================

    // Kotlin coroutines for Flow support
    implementation(libs.kotlinx.coroutines.core)

    // ========================================================================
    // Testing
    // ========================================================================
    testImplementation(libs.junit)
}
