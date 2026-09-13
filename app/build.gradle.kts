import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    // 注意：AGP 9 起内置 Kotlin 支持，不能再应用 org.jetbrains.kotlin.android
}

/*
 * 签名凭据不写进版本库。
 *
 * 读取顺序：local.properties（已被 .gitignore 忽略）-> 环境变量。
 *   local.properties 键名   kuaisnap.storeFile / storePassword / keyAlias / keyPassword
 *   环境变量名              KUAISNAP_STOREFILE / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
 *
 * 密钥库是应用的唯一身份：泄露则他人可签出系统认可的「官方更新」，
 * 丢失则永远无法覆盖安装更新（只能卸载重装）。请离线备份 keystore/ 目录。
 */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(localKey: String, envKey: String): String? =
    localProps.getProperty(localKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val defaultKeystore = "keystore/kuaisnapplus-release.jks"
val keystorePath = secret("kuaisnap.storeFile", "KUAISNAP_STOREFILE") ?: defaultKeystore
val keystorePassword = secret("kuaisnap.storePassword", "KUAISNAP_STORE_PASSWORD")
val keystoreAlias = secret("kuaisnap.keyAlias", "KUAISNAP_KEY_ALIAS") ?: "kuaisnapplus"
val keystoreKeyPassword = secret("kuaisnap.keyPassword", "KUAISNAP_KEY_PASSWORD")

// 只在真的要打包时才校验，避免 ./gradlew tasks、IDE 同步等被卡住
val packaging = gradle.startParameter.taskNames.any {
    it.contains("assemble", true) || it.contains("bundle", true) || it.contains("package", true)
}
if (packaging && (keystorePassword == null || keystoreKeyPassword == null)) {
    throw GradleException(
        """
        |缺少签名凭据，无法打包。
        |
        |请在 local.properties 中补齐（该文件已被 .gitignore 忽略）：
        |    kuaisnap.storePassword=<密钥库口令>
        |    kuaisnap.keyPassword=<密钥口令>
        |
        |或改用环境变量 KUAISNAP_STORE_PASSWORD / KUAISNAP_KEY_PASSWORD。
        |密钥库文件：$keystorePath
        """.trimMargin()
    )
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
            storeFile = rootProject.file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keystoreAlias
            keyPassword = keystoreKeyPassword
            // v3 覆盖 API 28+ 且支持签名密钥轮换，为日后换钥匙留后路；v1/v2 由 minSdk 自动决定
            enableV3Signing = true
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
