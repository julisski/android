plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // NOTE: this project deliberately does NOT apply the kotlin.serialization plugin.
    // The base sample needed it for @Serializable Navigation-3 keys; this MVVM lesson has
    // no navigation and no serializable state, so the plugin was removed to stay focused.
}

android {
    namespace = "com.example.mvvmstate"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.mvvmstate"
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
    // --- MVVM / state plumbing (the focus of THIS project) ---
    // viewModel() factory for Compose: obtains a NotesViewModel that survives recomposition
    // AND configuration changes (rotation), so its StateFlow keeps its state.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // collectAsStateWithLifecycle(): turns the ViewModel's StateFlow into Compose State while
    // honouring the lifecycle (it stops collecting when the screen is not visible).
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
