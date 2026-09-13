plugins {
    alias(libs.plugins.android.application)
    // AGP 9.0+ 内置 Kotlin，无需 org.jetbrains.kotlin.android
    alias(libs.plugins.compose.compiler)
    // kotlinx.serialization 编译器插件（为 @Serializable 生成序列化器）
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.oopnv70.uilab"
    // Android 17 (API 37) 引入了 minor 版本，SDK 平台真实包名/目录为 android-37.0
    // 必须同时指定 compileSdkMinor，否则 AGP 会去找不存在的 android-37 目录
    compileSdk = 37
    compileSdkMinor = 0

    // ------------------------------------------------------------------
    // 固定签名（解决「每次都要卸载重装」）
    //
    // 默认的 debug 构建会用一个「每次都由 Gradle 临时生成」的 debug keystore，
    // 导致每次 CI 产出的 APK 签名都不一样，Android 因此认为它们是不同应用，
    // 只能卸载后重装，数据和权限都会丢。
    //
    // 这里改为：签名信息从环境变量读取（CI 里由 GitHub Secrets 注入），
    // 只要 keystore 不变，每次构建的签名就完全一致，可以直接覆盖安装。
    //
    // 本地构建时若没有设置这些环境变量，就回退到默认 debug 签名，不影响开发。
    // ------------------------------------------------------------------
    val keystorePath = System.getenv("UILAB_KEYSTORE_PATH")
    val hasFixedSigning = keystorePath != null && file(keystorePath).exists()

    signingConfigs {
        if (hasFixedSigning) {
            create("fixed") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("UILAB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UILAB_KEY_ALIAS")
                keyPassword = System.getenv("UILAB_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.oopnv70.uilab"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        // 每次 UI 调整递增，便于确认手机上装的是哪一版构建
        versionName = "0.5-fixed-signing"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // 有固定签名就用固定签名；没有则什么都不设，
            // 让 AGP 用内置的 debug 签名（避免 getByName("debug") 的时序问题）。
            if (hasFixedSigning) {
                signingConfig = signingConfigs.getByName("fixed")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}
// 说明（AGP 9 内置 Kotlin）：
// 官方文档明确：使用内置 Kotlin 时无需设置 kotlin.compilerOptions.jvmTarget，
// 其值默认取 android.compileOptions.targetCompatibility（此处为 17）。

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // 网络：Retrofit + OkHttp + kotlinx.serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 本地存储
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
}