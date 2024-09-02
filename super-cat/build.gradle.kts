plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("plugin.serialization").version("1.9.20")
    id("org.jetbrains.dokka")
}

group = "com.aiocare.supercat"
version = "3.0.6"

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
//    js {
//        useCommonJs()
//        moduleName = "super-cat"
//        browser {
//            testTask {
//                useKarma {
//                    useChrome()
//                }
//            }
//            webpackTask {
//                outputFileName = "aiocare_super_cat.js"
//                output.library = "aiocare-super-cat"
//            }
//        }
//        binaries.library()
//        generateTypeScriptDefinitions()
//    }

    ktlint {
        disabledRules.set(setOf("no-wildcard-imports"))
    }

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "super-cat"
            isStatic = true
        }
    }

//    cocoapods {
//        summary = "Some description for the super-cat"
//        homepage = "Link to the super-cat Module homepage"
//        version = "1.0"
//        ios.deploymentTarget = "13.0"
//        framework {
//            baseName = "super-cat"
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
//                implementation("com.aiocare:sdk:0.7.44")
                implementation("com.aiocare:base-shared-data:0.7.46-extended_delay")
                implementation("com.aiocare:bluetooth:0.7.46-extended_delay")
                implementation("com.aiocare:command:0.7.46-extended_delay")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
//                implementation("com.aiocare.sdk:spirometer-sdk:0.0.8")
//                implementation("com.aiocare.models:common-models:+")
                implementation("com.juul.kable:core:0.25.1")
                implementation("com.squareup.okio:okio:3.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")

                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-client-logging:2.3.5")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val mobileMain by creating {
            dependsOn(commonMain)
        }
        val mob by creating {
            dependsOn(commonTest)
        }

        val androidMain by getting {
            dependsOn(mobileMain)
            dependencies {
                implementation("com.google.code.gson:gson:2.10.1")
            }
        }
        val androidUnitTest by getting
//        val jsMain by getting {
//        }
//        val jsTest by getting {
//            dependencies {
//                implementation("org.jetbrains.kotlin:kotlin-test-js")
//            }
//        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(mobileMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.aiocare.supercat"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}
