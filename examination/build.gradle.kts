plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization").version("1.9.20")
    id("com.android.library")
}

android{
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
        useLiveLiterals = false
    }
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "3.0.9"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "examination"
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {

//                implementation("com.aiocare.sdk:spirometer-sdk:+")
                implementation(project(":super-cat"))
                implementation("com.aiocare:bluetooth:0.7.46-extended_delay")
                implementation("com.aiocare:sdk:0.7.46-extended_delay")
//                implementation("com.aiocare.cortex:cortex:+")
//                implementation("com.aiocare.models:common-models:+")
                implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
                implementation("com.jstarczewski.kstate:kstate-core:0.0.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("com.patrykandpatrick.vico:core:1.8.0")
                implementation("com.patrykandpatrick.vico:compose:1.8.0")
                implementation(kotlin("reflect"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
                implementation("io.ktor:ktor-client-logging:2.3.5")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("androidx.core:core-ktx:1.10.1")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation(platform("androidx.compose:compose-bom:2023.06.01"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.navigation:navigation-compose:2.6.0")
                implementation("io.ktor:ktor-client-android:2.3.5")
            }
        }
    }
}

android {
    namespace = "com.aiocare.examination"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}
dependencies {
//    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation(project(mapOf("path" to ":old-cortex")))
}