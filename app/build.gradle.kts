plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tw.smilenalife.pangwallet.v2"
    compileSdk = 35

    defaultConfig {
        applicationId = "tw.smilenalife.pangwallet.v2"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "2.1"
    }
}

kotlin {
    jvmToolchain(17)
}
