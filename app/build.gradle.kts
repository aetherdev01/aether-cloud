plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// ── Signing config from local.properties ─────────────────────────────────────
private fun localProp(key: String): String? {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return null
    val props = java.util.Properties().also { it.load(f.inputStream()) }
    return props.getProperty(key)
}

android {
    namespace   = "com.aether.cloud"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "com.aether.cloud"
        minSdk          = 24
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0.0"

        // APK output name: aether-cloud-v1.0.0-release.apk
        base.archivesName = "aether-cloud-v$versionName"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProp("STORE_FILE")
            if (storeFilePath != null) {
                storeFile     = file(storeFilePath)
                storePassword = localProp("STORE_PASSWORD")
                keyAlias      = localProp("KEY_ALIAS")
                keyPassword   = localProp("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

dependencies {
    // ── Core ─────────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── Compose ───────────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // ── Activity & Navigation ─────────────────────────────────────────────────
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Room ──────────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Image Loading ─────────────────────────────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.glide)

    // ── Accompanist ───────────────────────────────────────────────────────────
    implementation(libs.accompanist.systemuicontroller)

    // ── Utils ─────────────────────────────────────────────────────────────────
    implementation(libs.gson)

    // ── Firebase ──────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    // ── Google Sign-In ────────────────────────────────────────────────────────
    implementation(libs.play.services.auth)

    // ── Facebook Login ────────────────────────────────────────────────────────
    implementation(libs.facebook.login)

    // ── Ads ───────────────────────────────────────────────────────────────────
    implementation(libs.unity.ads)
}
