// ============================================================================
// SMS-UPSTREAM MODULE
// ============================================================================
//
// This module contains VENDORED AOSP Messaging source code.
//
// RULES:
// 1. Keep modifications MINIMAL
// 2. Document ALL changes in PATCHES.md
// 3. NO Matrix code
// 4. NO GrapheneOS-specific code
// 5. NO app-specific configuration
// 6. ONLY remove/stub hidden APIs
// 7. ONLY wire to core-sms interfaces where necessary
//
// When upstream updates:
// 1. Replace src/ with new AOSP source
// 2. Re-apply patches from PATCHES.md
// 3. Fix build errors
// 4. Update PATCHES.md with any new changes
//
// ============================================================================

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.messaging"
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

    // AOSP uses Java, not Kotlin
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            aidl.srcDirs("src/main/aidl")
        }
    }

    lint {
        // Upstream code may have lint issues we cannot fix
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // AndroidX replacements for support library (upstream uses support lib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.annotation)

    // Material (upstream uses material components)
    implementation(libs.material)

    // Core-SMS interfaces (adapter layer)
    implementation(project(":core-sms"))
}
