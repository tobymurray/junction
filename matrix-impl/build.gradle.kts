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
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
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
    // Note: Media functionality is included in trixnity-client-media-okio
    // ========================================================================
    implementation(libs.trixnity.client)
    // implementation(libs.trixnity.client.media) // Not available in 4.22.7

    // Ktor HTTP client engine (required by Trixnity for network requests)
    implementation(libs.ktor.client.okhttp)

    // ========================================================================
    // Room database for room mapping
    // ========================================================================
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

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
