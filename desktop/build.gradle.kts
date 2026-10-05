import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(project(":shared"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

compose.desktop {
    application {
        mainClass = "com.example.rulearn.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "RuLearn"
            packageVersion = "1.0.0"
            description = "RuLearn — 俄语学习（随身听 / 拼写 / 测验）"
            vendor = "Davey"
            copyright = "© 2026 Davey"
            // 产物输出到 build/compose/binaries/main-release/，跨平台（Windows/macOS）一致，便于 CI 收集。
            macOS {
                bundleID = "com.example.rulearn.desktop"
            }
        }
    }
}
