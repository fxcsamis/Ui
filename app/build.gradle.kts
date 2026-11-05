plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.arkitak.lite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.arkitak.lite"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // WebSocket client for the Gemini Live conversation (same version as the main ARKITAK app)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Wake engine B (Vosk): offline recognition of the two names. The model goes in app/src/main/assets/model-hi/ (see README).
    implementation("com.alphacephei:vosk-android:0.3.47@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}
