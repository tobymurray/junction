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
            java.directories.add("src/main/java")
            res.directories.add("src/main/res")
            aidl.directories.add("src/main/aidl")
            assets.directories.add("src/main/assets")
        }
    }

    lint {
        // Upstream code may have lint issues we cannot fix
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // AndroidX (replacing support library)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    // Additional AndroidX needed by AOSP Messaging
    implementation("androidx.viewpager:viewpager:1.1.0")
    implementation("androidx.cursoradapter:cursoradapter:1.0.0")
    implementation("androidx.loader:loader:1.1.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")

    // Material
    implementation(libs.material)

    // Guava (used extensively by AOSP Messaging)
    implementation("com.google.guava:guava:33.3.1-android")

    // libphonenumber (phone number parsing/formatting)
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.50")

    // Core-SMS interfaces (adapter layer)
    implementation(project(":core-sms"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)

    // NOTE: The following AOSP-internal libraries are NOT available as public dependencies:
    // - androidx.appcompat.mms.* (MMS PDU handling)
    // - com.android.ex.chips.* (contact chips)
    // - com.android.ex.photo.* (photo viewer)
    // - com.android.vcard.* (vCard parsing)
    // - android.support.rastermill.* (frame sequence)
    // - com.android.common.contacts.* (data usage stat updater)
    //
    // These must be either:
    // 1. Stubbed out
    // 2. Replaced with public alternatives
    // 3. Copied from AOSP as separate modules
}
