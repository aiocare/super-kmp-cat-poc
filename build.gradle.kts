plugins {
    // trick: for the same plugin versions in all sub-modules
    id("com.android.library").version("8.1.0").apply(false)
    kotlin("multiplatform").version("1.9.0").apply(false)
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.android.application") version "8.1.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    id("org.jetbrains.dokka") version "1.8.20"
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false

}
