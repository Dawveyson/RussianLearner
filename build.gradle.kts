plugins {
    // AGP 9.x is required to target compileSdk/targetSdk 37 (Android 17);
    // it also matches the Gradle 9.3 wrapper already in use.
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
