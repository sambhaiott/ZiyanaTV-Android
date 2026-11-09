plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ziyana.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ziyana.tv"
        minSdk = 23
        targetSdk = 28
        versionCode = 4
        versionName = "1.3"
    }

    signingConfigs {
        create("stableDebug") {
            storeFile = file(System.getenv("RUNNER_TEMP") + "/ziyana-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
