plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    // 注意：AGP 9 起内置 Kotlin 支持，不能再应用 org.jetbrains.kotlin.android
}

android {
    namespace = "com.kite.kuaisnapplus"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.kite.kuaisnapplus"
        // Miuix 要求 minSdk >= 23；LSPosed 本身要求 Android 8.1+
        minSdk = 23
        // 保持与旧版本一致，避免分区存储等行为变化
        targetSdk = 26
        versionCode = 20260912
        versionName = "1.3.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/kuaisnapplus-release.jks")
            storePassword = "KuaiSnap#2026Release"
            keyAlias = "kuaisnapplus"
            keyPassword = "KuaiSnap#2026Release"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

base {
    archivesName.set("KuaiSnapPlus_1.3.0")
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    compileOnly(fileTree("libs/compile_only") { include("*.jar") })

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
}
