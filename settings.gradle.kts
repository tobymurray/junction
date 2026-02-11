pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Junction"

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
// core-matrix/   → Matrix bridge interfaces (android-library)
//                  - Defines interfaces for Matrix bridge functionality
//                  - Room mapping, presence, message bridging
//                  - NO Matrix SDK dependencies
//
// matrix-impl/   → Matrix bridge implementation (android-library)
//                  - Implements core-matrix interfaces
//                  - Uses Trixnity SDK
//                  - Room database for mapping persistence
//
// core-persistence/ → Database persistence layer (android-library)
//                  - Room database for message mapping and deduplication
//                  - Conversation-aware persistence (supports group messages)
//                  - Multi-participant tracking for group MMS/SMS
//                  - Used by app and matrix-impl modules
//
// app/           → Main application (android-application)
//                  - UI, Matrix bridge orchestration, GrapheneOS integration
//                  - Depends ONLY on core-sms and core-matrix interfaces
//                  - NEVER imports from sms-upstream or matrix-impl directly
//
// ============================================================================

include(":sms-upstream")
include(":core-sms")
include(":core-matrix")
include(":core-persistence")
include(":matrix-impl")
include(":app")
