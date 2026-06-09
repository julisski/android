plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // KSP (Kotlin Symbol Processing) — REQUIRED so Room's annotation processor runs during
    // assembleDebug and GENERATES the concrete NoteDatabase_Impl / NoteDao_Impl classes.
    // Without this plugin applied, the ksp("androidx.room:room-compiler") dependency below
    // does nothing and the app fails at runtime ("cannot find implementation for NoteDatabase").
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.roomandpreferences"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.roomandpreferences"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- LOCAL PERSISTENCE STACK (the whole point of this project) ---
    // Room: structured/relational storage for our Note rows.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // The Room compiler is wired through KSP (NOT implementation). This is the line that
    // makes KSP generate the SQLite-backed implementation of @Dao / @Database at build time.
    ksp(libs.androidx.room.compiler)
    // DataStore Preferences: simple settings (dark theme + sort order). Replaces SharedPreferences.
    implementation(libs.androidx.datastore.preferences)
    // Compose + ViewModel/Flow glue: viewModel() and collectAsStateWithLifecycle().
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
