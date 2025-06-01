plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mimeng.chess"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mimeng.chess"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 读取.env文件内容并注入到BuildConfig（Kotlin DSL）
        println("Loading environment variables from .env file")
        val envFile = rootProject.file("app/.env")
        println("envFile absolute path: ${envFile.absolutePath}")
        if (envFile.exists()) {
            envFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val (key, value) = trimmed.split("=", limit = 2)
                    val validKey = key.trim().replace("[^A-Za-z0-9_]".toRegex(), "_")
                    buildConfigField("String", validKey, "\"${value.trim()}\"")
                    println("Injected env: $validKey = ${value.trim()}")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.socketio)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

