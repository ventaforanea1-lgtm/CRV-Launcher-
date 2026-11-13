plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.eduardo.crvlauncher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.eduardo.crvlauncher"
        minSdk = 26
        targetSdk = 28
        versionCode = 3
        versionName = "3.0"
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
