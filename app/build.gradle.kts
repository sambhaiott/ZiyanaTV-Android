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
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "device"
    productFlavors {
        create("tv") {
            dimension = "device"
            applicationId = "com.ziyana.tv"
            versionNameSuffix = "-tv"
        }
        create("mobile") {
            dimension = "device"
            applicationId = "com.ziyana.mobile"
            versionNameSuffix = "-mobile"
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
