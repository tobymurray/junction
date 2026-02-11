// ============================================================================
// APP MODULE - Main Application
// ============================================================================
//
// This module contains:
// - Application entry point and UI
// - BroadcastReceivers for SMS/MMS (must be here for manifest)
// - Role/permission request handling
// - GrapheneOS-specific adaptations
// - Future: Matrix bridge integration
//
// DEPENDENCY RULES:
// - Depends on core-sms for interfaces
// - Depends on sms-upstream for implementation
// - NEVER import directly from com.android.messaging.* (upstream internals)
// - All upstream access goes through core-sms interfaces
//
// ============================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.technicallyrural.junction.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.technicallyrural.junction"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
        viewBinding = true
    }
}

dependencies {
    // ========================================================================
    // Module dependencies
    // ========================================================================

    // Core SMS interfaces (adapter layer) - PRIMARY DEPENDENCY
    implementation(project(":core-sms"))

    // Upstream AOSP implementation - provides implementations of core-sms interfaces
    // App should NOT import directly from this module's packages
    implementation(project(":sms-upstream"))

    // Core Matrix interfaces and implementation
    implementation(project(":core-matrix"))
    implementation(project(":matrix-impl"))

    // Persistence layer (Room database for message mapping)
    implementation(project(":core-persistence"))

    // ========================================================================
    // AndroidX
    // ========================================================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    // Material Design
    implementation(libs.material)

    // ========================================================================
    // Testing
    // ========================================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
}
