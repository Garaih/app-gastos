plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Código de versión = minutos desde 2026-01-01 (UTC). Crece solo en cada compilación,
// tanto en Android Studio como en GitHub Actions, así una versión nueva siempre se
// instala encima de la anterior. GitHub Actions lo pasa en VERSION_CODE.
val codigoVersion: Int = System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: ((System.currentTimeMillis() / 1000 - 1767225600L) / 60).toInt()

android {
    namespace = "com.garaih.appgastos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.garaih.appgastos"
        minSdk = 26
        targetSdk = 36
        versionCode = codigoVersion
        versionName = "0.4.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
