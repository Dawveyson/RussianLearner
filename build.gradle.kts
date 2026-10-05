plugins {
    // AGP 9.x is required to target compileSdk/targetSdk 37 (Android 17);
    // it also matches the Gradle 9.3 wrapper already in use.
    // 版本在 settings.gradle.kts 的 pluginManagement 中统一声明。
    id("com.android.application") apply false
}
