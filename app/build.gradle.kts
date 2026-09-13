plugins {
    alias(libs.plugins.android.application)
    // AGP 9.0+ 内置 Kotlin，无需 org.jetbrains.kotlin.android
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "com.oopnv70.uilab"
    // Android 17 (API 37) 引入了 minor 版本，SDK 平台真实包名/目录为 android-37.0
    // 必须同时指定 compileSdkMinor，否则 AGP 会去找不存在的 android-37 目录
    compileSdk = 37
    compileSdkMinor = 0
    defaultConfig {
        applicationId = "com.oopnv70.uilab"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        // 每次 UI 调整递增，便于确认手机上装的是哪一版构建
        versionName = "0.2-nav"
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
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
    debugImplementation(libs.androidx.ui.tooling)
}