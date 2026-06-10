plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // NOTE: like the other concept demos, this project applies only the Android +
    // Compose plugins. There is no @Serializable state here, so kotlin.serialization
    // is intentionally NOT applied.
}

android {
    namespace = "com.example.locationservices"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.locationservices"
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

    // --- LOCATION SERVICES (this project's concept) --------------------------
    // FusedLocationProviderClient: the modern, battery-efficient device-location API.
    implementation(libs.play.services.location)

    // --- ViewModel + StateFlow -> Compose ------------------------------------
    // viewModel() inside a @Composable.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // collectAsStateWithLifecycle() to observe StateFlow<LocationUiState>.
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    // Lets the unit tests run suspend repository code via runTest { }.
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
