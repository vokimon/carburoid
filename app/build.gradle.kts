// App module build file
import java.util.Properties
import java.io.FileInputStream
import com.android.build.api.variant.ApkOutput
import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    id("net.canvoki.gradle.android-yaml-strings") version "1.0.0"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    FileInputStream(versionPropsFile).use { load(it) }
}

fun loadEnv() {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return
    envFile.forEachLine { line ->
        if (line.trim().isEmpty() || line.startsWith("#")) return@forEachLine
        val parts = line.split("=", limit = 2)
        if (parts.size == 2) {
            project.ext.set(parts[0].trim(), parts[1].trim())
        }
    }
}

loadEnv()

android {
    signingConfigs {
        create("release") {
            storeFile = file(project.property("RELEASE_STORE_FILE").toString())
            storePassword = project.property("RELEASE_STORE_PASSWORD").toString()
            keyAlias = project.property("RELEASE_KEY_ALIAS").toString()
            keyPassword = project.property("RELEASE_KEY_PASSWORD").toString()
        }
    }

    namespace = "net.canvoki.carburoid"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.canvoki.carburoid"
        minSdk = 26
        targetSdk = 36
        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                val version = mergedFlavor.versionName
                val buildType = buildType.name
                val appId = applicationId
                val flavorInfix = if (android.productFlavors.size > 1) "-$flavorName" else ""
                outputFileName = "${appId}${flavorInfix}-${version}-${buildType}.apk"
            }
        }
    }

    bundle {
        language {
            // Because user may change the language, all should be present
            enableSplit = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "freeness"

    productFlavors {
        create("floss") {
            dimension = "freeness"
            isDefault = true
        }
        create("nonfree") {
            dimension = "freeness"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xnested-type-aliases")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    testOptions {
        unitTests.all {
            it.jvmArgs(
                "-XX:+EnableDynamicAgentLoading",
                "--add-opens", "java.base/java.time=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
            )
        }
    }
    // Dependencies info is encrypted Google only to read block
    dependenciesInfo {
        includeInApk = false // APK
        includeInBundle = false // AAB
    }
}

tasks.withType<Test> {
    testLogging {
        events("failed", "skipped")//, "standardOut", "standardError", "passed", "started"
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

dependencies {
    implementation(project(":shared"))

    // Platform BOM imports
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(platform(libs.kotlinx.serialization.bom))
    implementation(platform(libs.ktor.bom))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.kotlinx.serialization.json)

    // Compose
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Other libraries
    implementation(libs.osmdroid.android)
    implementation(libs.osmdroid.wms)
    implementation(libs.okhttp)
    implementation(libs.koalaplot.core.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Non-free flavor
    "nonfreeImplementation"(libs.play.services.location)

    // Tes
    testImplementation(libs.junit)
    testImplementation(libs.java.diff.utils)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)      // Version from coroutines BOM
    //testImplementation(libs.kotlinx.serialization.json) // Version from serialization BOM
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
