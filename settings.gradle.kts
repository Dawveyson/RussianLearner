pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
    // 统一所有模块的插件版本，避免多模块各自声明导致的
    // "plugin already on the classpath with an unknown version" 冲突。
    // Android(:app) 与 桌面(:desktop) 必须共用同一 Kotlin 版本。
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
        id("org.jetbrains.compose") version "1.9.1"
        id("com.android.application") version "9.2.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}

rootProject.name = "RuLearn"
include(":app")
include(":shared")
include(":desktop")
