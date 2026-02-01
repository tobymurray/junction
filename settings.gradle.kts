pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AospMessaging"

// ============================================================================
// MODULE STRUCTURE
// ============================================================================
//
// sms-upstream/  → Vendored AOSP Messaging (android-library)
//                  - Minimal modifications
//                  - Tracks upstream AOSP
//                  - NO Matrix, NO GrapheneOS, NO app-specific logic
//
// core-sms/      → Adapter/Anti-corruption layer (android-library)
//                  - Defines interfaces for SMS transport, storage, notifications
//                  - Facades that wrap upstream implementation
//                  - Shields app from upstream internals
//
// app/           → Main application (android-application)
//                  - UI, Matrix bridge, GrapheneOS integration
//                  - Depends ONLY on core-sms interfaces
//                  - NEVER imports from sms-upstream directly
//
// ============================================================================

include(":sms-upstream")
include(":core-sms")
include(":app")
