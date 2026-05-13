plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ben.inly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ben.inly"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Shrinks and obfuscates code
            isShrinkResources = true // Removes unused resources
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

    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.composables:icons-lucide:1.1.0")
    implementation(libs.haze)

    // Architecture
    implementation(libs.hilt.android)
    "ksp"(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    // Data & Security
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    "ksp"(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)

    // SQLCipher for Room Database Encryption
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Security Crypto for hardware-backed key storage
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Voice models and NLP
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.32")
    implementation("com.joestelmach:natty:0.13")
}