plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("net.canvoki.gradle.android-yaml-strings") version "1.0.0"
}

android {
    namespace = "net.canvoki.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    //implementation(libs.androidx.compose.ui.tooling.preview)
    //debugImplementation(libs.androidx.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.activity.compose)
    //implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.datastore.preferences)
    //implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.preference.ktx)

    // Other libraries
    //implementation(libs.osmdroid.android)
    //implementation(libs.osmdroid.wms)
    //implementation(libs.okhttp)
    //implementation(libs.koalaplot.core.android)
    //implementation(libs.kotlinx.serialization.json)
    //implementation(libs.ktor.client.core)
    //implementation(libs.ktor.client.cio)
    //implementation(libs.ktor.client.content.negotiation)

}
