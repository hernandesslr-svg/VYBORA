plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.vybora.android"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val path = System.getenv("VYBORA_KEYSTORE_PATH")
            if (!path.isNullOrBlank()) storeFile = file(path)
            System.getenv("VYBORA_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }?.let { storePassword = it }
            System.getenv("VYBORA_KEY_ALIAS")?.takeIf { it.isNotBlank() }?.let { keyAlias = it }
            System.getenv("VYBORA_KEY_PASSWORD")?.takeIf { it.isNotBlank() }?.let { keyPassword = it }
        }
    }

    defaultConfig {
        applicationId = "app.vybora.android"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "0.3.0"
        // Troque pela URL HTTPS do backend publicado antes de gerar o APK.
        buildConfigField("String", "API_BASE_URL", "\"https://vybora-production.up.railway.app\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures { compose = true; buildConfig = true }

    buildTypes {
        getByName("release") { signingConfig = signingConfigs.getByName("release") }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
