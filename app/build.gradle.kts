plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "com.leekleak.kvaesitsometeolt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.leekleak.kvaesitsometeolt"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        signingConfig = signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = true
                isOptimizeCode = true
                proguardFile("proguard-rules.pro")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.plugin.sdk)
    implementation(libs.commons.suncalc)
}