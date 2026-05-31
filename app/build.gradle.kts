plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.generateKotlin", "true")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        mainRun {
            mainClass.set("com.ben.inly.DesktopMainKt")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)

                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
                implementation(libs.androidx.room.runtime)
                implementation(libs.haze)
                implementation(libs.koin.compose.multiplatform)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.datetime)
                implementation(libs.coil.compose)
                implementation(libs.navigation.compose.kmp)
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.okio)

                implementation("io.ktor:ktor-client-core:3.3.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
                implementation("io.ktor:ktor-client-auth:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.androidx.room.ktx)
                implementation(libs.sqlcipher)
                implementation(libs.androidx.sqlite.ktx)
                implementation(libs.androidx.security.crypto)
                implementation("io.coil-kt.coil3:coil-compose:3.3.0")
                implementation("com.composables:icons-lucide:1.1.0")
                implementation("com.joestelmach:natty:0.13")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
                implementation("org.jsoup:jsoup:1.17.2")
                implementation("io.ktor:ktor-client-okhttp:3.3.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
                implementation("io.ktor:ktor-client-auth:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")

                // CameraX
                implementation("androidx.camera:camera-camera2:1.6.0")
                implementation("androidx.camera:camera-lifecycle:1.6.0")
                implementation("androidx.camera:camera-view:1.6.0")

                // ML Kit Barcode Scanning
                implementation("com.google.mlkit:barcode-scanning:17.3.0")
                implementation("com.google.guava:guava:33.4.8-android")

                implementation("androidx.core:core-splashscreen:1.2.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jsoup:jsoup:1.17.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
                implementation("io.ktor:ktor-client-java:3.3.0")
                implementation("io.ktor:ktor-server-netty:3.3.0")
                implementation("io.ktor:ktor-server-content-negotiation:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
                implementation("io.ktor:ktor-server-auth:3.3.0")
                implementation("org.jmdns:jmdns:3.5.9")
                implementation("com.google.zxing:core:3.5.3")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ben.inly.DesktopMainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "Inly"
            packageVersion = "1.0.0"
        }
    }
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
            isMinifyEnabled = true
            isShrinkResources = true
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

    androidResources {
        noCompress += listOf("so", "mdl", "fst", "conf", "int", "dubm", "ie", "mat", "stats")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    sourceSets["main"].assets.srcDirs("src/androidMain/assets")
    sourceSets["main"].java.srcDirs("src/androidMain/kotlin")
}

dependencies {
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.skiko") {
                useVersion("0.9.37.4")
            }
        }
    }
}