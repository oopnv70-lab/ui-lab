# 保留 Compose 相关（通常 R8 已能正确处理，这里只做必要声明）
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
