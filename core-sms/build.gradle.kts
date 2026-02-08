// ============================================================================
// CORE-SMS MODULE - Adapter / Anti-Corruption Layer
// ============================================================================
//
// This module defines INTERFACES and FACADES that:
// 1. Abstract upstream AOSP implementation details
// 2. Provide stable API for app module
// 3. Allow upstream replacement without app changes
//
// DEPENDENCY RULES:
// - This module has NO dependency on sms-upstream
// - This module defines interfaces that sms-upstream IMPLEMENTS
// - This module defines facades that WRAP upstream functionality
// - App module depends ONLY on this module's interfaces
//
// INTERFACE CATEGORIES:
// - SmsTransport: Send/receive SMS and MMS
// - MessageStore: Access conversation and message data
// - NotificationFacade: Display message notifications
// - ContactResolver: Resolve phone numbers to contacts
//
// ============================================================================

plugins {
    alias(libs.plugins.android.library)
    // Note: Kotlin plugin not needed - AGP 9.0+ has built-in Kotlin support
}

android {
    namespace = "com.technicallyrural.junction.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    // AGP 9.0+ configures Kotlin through compileOptions (jvmTarget matches targetCompatibility)
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)

    // Lifecycle for observable data
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    // Coroutines for async operations
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
