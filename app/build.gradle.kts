plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.rulearn"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.rulearn"
        minSdk = 24
        targetSdk = 37
        versionCode = 13
        versionName = "1.0.4"
    }

    signingConfigs {
        create("release") {
            // 签名信息从环境变量或 ~/.gradle/gradle.properties 读取，切勿把密码写进仓库。
            // 空字符串也视为“未配置”，回退到 debug 签名（便于 CI / 开源协作者直接构建）。
            val ks = (System.getenv("RU_KEYSTORE") ?: project.findProperty("RU_KEYSTORE") as? String)?.takeIf { it.isNotBlank() }
            val pw = (System.getenv("RU_KEYSTORE_PASSWORD") ?: project.findProperty("RU_KEYSTORE_PASSWORD") as? String)?.takeIf { it.isNotBlank() }
            val alias = (System.getenv("RU_KEY_ALIAS") ?: project.findProperty("RU_KEY_ALIAS") as? String)?.takeIf { it.isNotBlank() }
            val kpw = (System.getenv("RU_KEY_PASSWORD") ?: project.findProperty("RU_KEY_PASSWORD") as? String)?.takeIf { it.isNotBlank() }
            if (ks != null && pw != null && alias != null && kpw != null) {
                storeFile = file(ks)
                storePassword = pw
                keyAlias = alias
                keyPassword = kpw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 正式发布必须用 release 签名（读取 ~/.gradle/gradle.properties 或环境变量中的
            // RU_KEYSTORE / RU_KEYSTORE_PASSWORD / RU_KEY_ALIAS / RU_KEY_PASSWORD）。
            // 只有在显式设置 RU_ALLOW_DEBUG_SIGNING=true 时才允许回退 debug 签名，
            // 避免"以为发了正式版、其实是 debug 签名"的情况静默发生。
            val releaseSigning = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
            val allowDebug = (project.findProperty("RU_ALLOW_DEBUG_SIGNING") as? String)
                ?.equals("true", ignoreCase = true) == true
            signingConfig = releaseSigning ?: if (allowDebug) {
                signingConfigs.getByName("debug")
            } else {
                // 没配置正式签名就直接失败：发 Release 必须是正式签名
                throw GradleException(
                    "缺少正式签名配置：请在 ~/.gradle/gradle.properties 或环境变量中设置 " +
                        "RU_KEYSTORE / RU_KEYSTORE_PASSWORD / RU_KEY_ALIAS / RU_KEY_PASSWORD。" +
                        "若只是本地自测，可显式设置 -PRU_ALLOW_DEBUG_SIGNING=true 允许回退 debug 签名。"
                )
            }
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    // 词条配图（img 字段）加载
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
