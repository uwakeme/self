import java.io.File
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.self.profile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.self.profile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release 签名从环境变量读（勿提交 keystore）：
    //   SELF_RELEASE_STORE_FILE / SELF_RELEASE_STORE_PASSWORD / SELF_RELEASE_KEY_ALIAS / SELF_RELEASE_KEY_PASSWORD
    // 未配置时 assembleRelease 产出 unsigned APK，不阻塞构建。
    signingConfigs {
        create("release") {
            val storeFileEnv = System.getenv("SELF_RELEASE_STORE_FILE")
            if (storeFileEnv != null && File(storeFileEnv).exists()) {
                storeFile = File(storeFileEnv)
                storePassword = System.getenv("SELF_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("SELF_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("SELF_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // lint 门禁（2026-10 接入 CI 时加）。
    // 存量情况：lintDebug 当前 60 条（56 warning + 4 information / 0 error），
    // 所以没有建 baseline —— 有了 baseline 只会把同 ID 的新问题一起吞掉。
    // 有 error 就直接断构建，warning 先只报告。
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.androidx.compiler)

    // Coil
    implementation(libs.coil.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Biometric
    implementation(libs.androidx.biometric)

    // kotlinx.serialization
    implementation(libs.kotlinx.serialization.json)

    // 农历
    implementation(libs.lunar.java)

    // 单元测试（JUnit 5 + mockk；import 用 org.junit.jupiter.api.Test，禁混 JUnit 4）
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)

    // 设备端测试（AndroidJUnit4 / JUnit 4，与上面的 JVM 单测是两套体系，不要混）
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

tasks.withType<Test>().configureEach {
    // 只给 JVM 单测开 JUnit 5 平台。instrumentation 测试走 AndroidJUnit4（JUnit 4），
    // 万一被 useJUnitPlatform() 接管，androidTest 用例会被静默跳过（build 照样绿）。
    if (name.endsWith("UnitTest")) {
        useJUnitPlatform()
    }
}
