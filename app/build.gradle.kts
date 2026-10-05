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
        versionCode = 9
        versionName = "1.0.0"
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
            // 仅当提供了签名信息时才用 release 签名；否则回退 debug 签名，方便开源协作者直接构建。
            signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
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
