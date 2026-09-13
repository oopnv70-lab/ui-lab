// Top-level build file
// 注意：AGP 9.0+ 已内置 Kotlin 支持，不再需要 org.jetbrains.kotlin.android 插件。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}