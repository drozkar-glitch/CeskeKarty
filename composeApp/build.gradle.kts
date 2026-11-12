import java.io.File
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val iosTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
        }
    }
}

val ghRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val ciBuildNumber = ghRunNumber + 100
val appSemanticVersion = System.getenv("APP_VERSION")?.trim()?.takeIf { it.isNotEmpty() } ?: "1.0"

val cmKeystorePath = System.getenv("CM_KEYSTORE_PATH")?.trim().orEmpty()
val cmKeystoreFile = if (cmKeystorePath.isNotEmpty()) File(cmKeystorePath) else null
val hasCiKeystore = cmKeystoreFile != null && cmKeystoreFile.exists()

android {
    namespace = "cz.mares.ceskekarty"
    compileSdk = 36

    defaultConfig {
        applicationId = "cz.mares.ceskekarty"
        minSdk = 24
        targetSdk = 36
        versionCode = ciBuildNumber
        versionName = appSemanticVersion
    }

    signingConfigs {
        if (hasCiKeystore && cmKeystoreFile != null) {
            create("ciSigning") {
                storeFile = cmKeystoreFile
                storePassword = System.getenv("CM_KEYSTORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("CM_KEY_ALIAS").orEmpty()
                keyPassword = System.getenv("CM_KEY_PASSWORD").orEmpty()
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasCiKeystore) {
                signingConfig = signingConfigs.getByName("ciSigning")
            }
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasCiKeystore) {
                signingConfig = signingConfigs.getByName("ciSigning")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
