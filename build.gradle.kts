// Project level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
    id("com.github.ben-manes.versions") version "0.52.0"
}

spotless {
    kotlin {
        target(
            "app/src/**/*.kt",
            "buildSrc/src/**/*.kt"
        )
        ktlint("1.7.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
