// App module build file
import java.util.Properties
import java.io.FileInputStream
import com.android.build.api.variant.ApkOutput
import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    id("net.canvoki.gradle.android-yaml-strings") version "1.0.0"
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
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
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:latest")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:latest")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("org.osmdroid:osmdroid-wms:6.1.20")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("io.github.koalaplot:koalaplot-core-android:0.11.0")
    implementation("io.ktor:ktor-client-core:3.4.0")
    implementation("io.ktor:ktor-client-cio:3.4.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    "nonfreeImplementation"("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.code.gson:gson:2.13.2")
    testImplementation("io.github.java-diff-utils:java-diff-utils:4.12")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:latest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:latest")
    testImplementation("org.jetbrains.kotlin:kotlin-test:latest")
    testImplementation("org.robolectric:robolectric:4.16")

    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:latest")
    androidTestImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
}
