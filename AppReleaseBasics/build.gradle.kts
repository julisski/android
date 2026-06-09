// =============================================================================
// build.gradle.kts  (app module)
//
// CONCEPT FOCUS — RELEASE ENGINEERING BASICS:
//   This file is the heart of the "how do I ship this app?" lesson. Inspect:
//     • versionCode / versionName            — how you version a release.
//     • buildTypes { debug / release }        — the two build flavors and how
//                                               they differ (debuggable, minify).
//     • signingConfigs (COMMENTED example)    — where a release keystore +
//                                               passwords would be wired in.
//   The serialization + Navigation 3 plugins/deps from the base project were
//   removed because this project is about RELEASING, not navigating.
// =============================================================================

plugins {
    alias(libs.plugins.android.application)   // the Android application plugin (produces an APK/AAB)
    alias(libs.plugins.kotlin.compose)        // Kotlin Compose compiler plugin (required for @Composable)
    // NOTE: kotlin.serialization plugin removed — no @Serializable nav keys here.
}

android {
    // namespace = the package used to generate the R class and BuildConfig. It
    // does NOT have to equal applicationId, but here they match for simplicity.
    namespace = "com.example.appreleasebasics"
    compileSdk = 37                            // the SDK version the app is COMPILED against (use latest APIs)

    defaultConfig {
        // applicationId = the app's UNIQUE identity on the device and in the
        // Play Store. Once published you can NEVER change it. (namespace can
        // differ from this, but here they're intentionally the same.)
        applicationId = "com.example.appreleasebasics"
        minSdk = 24                            // oldest Android version the app will install on (API 24 = Android 7.0)
        targetSdk = 37                         // the API level the app is TESTED/optimized against (behavioral opt-ins)

        // -----------------------------------------------------------------
        // VERSIONING — the two numbers every release must set.
        //
        //   versionCode : an INTEGER that must INCREASE with every build you
        //                 upload. Google Play uses ONLY this to decide which
        //                 build is "newer". BUMP IT (e.g. 1 -> 2) on every
        //                 release you publish. Users never see it.
        //
        //   versionName : a human-readable STRING (e.g. "1.0", "1.2.3") shown
        //                 to users on the store listing and in Settings > Apps.
        //                 Bump it however your release process likes (semver is
        //                 common). It is NOT used by Play for ordering.
        // -----------------------------------------------------------------
        versionCode = 1                        // <-- bump this integer on EVERY published release
        versionName = "1.0"                    // <-- the version string users see; bump per your scheme

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // -------------------------------------------------------------------------
    // SIGNING CONFIGS — release-signing CONCEPTS (kept COMMENTED on purpose).
    //
    // Every APK/AAB must be cryptographically SIGNED. Debug builds are auto-
    // signed with a throwaway debug key, so they install but cannot be shipped.
    // A RELEASE build must be signed with YOUR OWN keystore — and Google uses
    // that signature to verify future updates come from you.
    //
    // The block below is intentionally commented out so that `assembleDebug`
    // builds WITHOUT requiring a real keystore file or any passwords. To enable
    // real release signing, you would:
    //   1. Create a keystore (see README "Publishing concepts"):
    //        keytool -genkeypair -v -keystore release.jks -alias myalias \
    //          -keyalg RSA -keysize 2048 -validity 10000
    //   2. Put the secrets OUTSIDE source control (e.g. ~/.gradle/gradle.properties
    //      or environment variables) — NEVER hardcode passwords in this file.
    //   3. Uncomment and wire it up, then reference it from buildTypes.release.
    //
    // signingConfigs {
    //     create("release") {
    //         storeFile = file(System.getenv("RELEASE_STORE_FILE") ?: "release.jks")
    //         storePassword = System.getenv("RELEASE_STORE_PASSWORD")
    //         keyAlias = System.getenv("RELEASE_KEY_ALIAS")
    //         keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    //     }
    // }
    // -------------------------------------------------------------------------

    buildTypes {
        // DEBUG build type: the default when you Run from the IDE or call
        // assembleDebug. It is auto-signed with the debug key, is debuggable,
        // and skips code shrinking so builds are fast. Never ship a debug build.
        debug {
            // (Defaults are fine for teaching. You could add applicationIdSuffix
            //  = ".debug" here to install debug + release side by side.)
            isMinifyEnabled = false            // no R8 shrinking in debug — faster builds, easier debugging
        }

        // RELEASE build type: what you ship to users.
        release {
            // For a REAL release you would normally turn these ON to shrink and
            // obfuscate the code (R8) and strip unused resources, making the app
            // smaller and harder to reverse-engineer:
            //     isMinifyEnabled = true
            //     isShrinkResources = true
            // We keep them OFF here so the teaching build stays simple and so
            // that this sample has no ProGuard/R8 rules to maintain.
            isMinifyEnabled = false            // TODO(real release): set true + add isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // To sign real releases, uncomment the signingConfigs block above and
            // attach it here so `bundleRelease` / `assembleRelease` produce a
            // signed artifact:
            //     signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true                         // turn on Jetpack Compose tooling for this module
    }
}

dependencies {
    // --- Jetpack Compose (BOM keeps all Compose artifact versions aligned) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)        // ComponentActivity + setContent + rememberLauncherForActivityResult
    implementation(libs.androidx.compose.material3)        // Material 3 components (Button, Text, Surface...)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)                 // provides ContextCompat.checkSelfPermission for the permission flow
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // NOTE: Navigation 3 + kotlinx-serialization deps were removed (not used here).

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
