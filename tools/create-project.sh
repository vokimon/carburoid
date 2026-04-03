#!/bin/bash

# create-android-kotlin-project.sh
# Creates a minimal Android Kotlin project structure from CLI, no IDE.
# sudo apt install
# Uses downloaded Gradle 8.7 to generate wrapper — Java 21 compatible.
# Prompts for project/package using 3-arg prompt_with_default.
# Detects Git repo in current or parent directories.

set -e  # Exit on any error

export TARGET_SDK=36
export MIN_SDK=26
export JAVA_VERSION=17

run() {
    echo -e "\033[34m:: $@\033[0m"
    "$@"
}
norun() {
    echo -e "Skipped: \033[33m:: $@\033[0m"
}

load_dotenv() {
    local dotenv_file="${1:-.env}"
    if [ -f "$dotenv_file" ]; then
        set -a
        source "$dotenv_file"
        set +a
    fi
}

update_dotenv() {
    local dotenv_file="$1"
    shift
    [ -f "$dotenv_file" ] || touch "$dotenv_file"

    local content
    content=$(<"$dotenv_file")

    for var_name in "$@"; do
        local value="${!var_name}"

        if grep -qE "^$var_name=" "$dotenv_file"; then
            # Escape sed special chars in value
            local escaped_value
            escaped_value=$(printf '%s\n' "$value" | sed 's/[\/&]/\\&/g')
            # Replace line in content variable
            content=$(printf '%s\n' "$content" | sed "s/^$var_name=.*/$var_name=\"$escaped_value\"/")
        else
            # Append new variable at end
            content="${content}"$'\n'"$var_name=\"$value\""
        fi
    done

    # Write all at once to file
    printf '%s\n' "$content" > "$dotenv_file"
}


parameter() {
    local var_name="$1"
    local cli_value="$2"
    local prompt_text="$3"
    local default_value="$4"

    # Reference to the variable we want to assign to
    declare -n ref="$var_name"

    local current_value="${!var_name}"

    if [ -n "$cli_value" ]; then
        echo "$var_name taken from command line: '$cli_value'"
        ref="$cli_value"
    elif [ -n "$current_value" ]; then
        echo "$var_name taken from .env"
        :
    else
        # Else ask the user, providing a default value
        read -p "$prompt_text ($var_name) [$default_value]: " user_input
        ref="${user_input:-$default_value}"
    fi
}

password_parameter() {
    local var_name="$1"
    local prompt_text="$2"

    # Reference to the variable we want to assign to
    declare -n ref="$var_name"

    local current_value="${!var_name}"

    if [ -n "$current_value" ]; then
        echo "$var_name taken from .env"
    else
        # Else ask the user, providing a default value
        read -sp "$prompt_text ($var_name): " user_input
        echo # Needed to go next line
        ref="${user_input}"
    fi
}

package_to_name() {
    local package="$1"
    local name="${package##*.}"     # get last part after last dot
    echo "${name^}"                 # capitalize first letter
}


install_system_dependencies() {
    norun sudo apt install \
        gradle \
        google-android-build-tools-35.0.1-installer \
        google-android-cmdline-tools-17.0-installer \
        google-android-emulator-installer \
        google-android-ndk-r26c-installer \
        google-android-platform-34-installer \
        google-android-platform-tools-installer \
        #
    export ANDROID_HOME=/usr/lib/android-sdk
}

in_git_repo() {
    if git rev-parse --git-dir > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

load_dotenv # Take existing params from previous executions as defaults
parameter PACKAGE_NAME "$1" "Enter package name" "com.example.dummy"
package_short_name=${PACKAGE_NAME##*.} # the last dotted separated part
parameter PROJECT_NAME "$2" "Enter project name" "$(package_to_name $PACKAGE_NAME)"
parameter RELEASE_STORE_FILE "" "Enter keystore path" "$(pwd)/${package_short_name}-release.p12"
password_parameter RELEASE_STORE_PASSWORD "Enter key store password"
parameter RELEASE_KEY_ALIAS "" "Enter key alias" "${package_short_name}"
password_parameter RELEASE_KEY_PASSWORD "Enter key password"
parameter AUTHOR_NAME "" "Enter your name" "$(getent passwd "$USER" | cut -d: -f5 | cut -d, -f1)"
parameter AUTHOR_COMPANY "" "Enter your company" ""
parameter AUTHOR_ROLE "" "Enter your role" ""
parameter AUTHOR_CITY "" "Enter your city" ""
parameter AUTHOR_STATE "" "Enter your state" ""
parameter AUTHOR_COUNTRY "" "Enter your contry ISO code" ""

 #RELEASE_KEY_CN="CN=David García Garzón, OU=Unknown, O=Unknown, L=Sant Joan Despí, ST=Barcelona, C=ES"


update_dotenv .env \
    AUTHOR_NAME \
    AUTHOR_COMPANY \
    AUTHOR_ROLE \
    AUTHOR_CITY \
    AUTHOR_STATE \
    AUTHOR_COUNTRY \
    RELEASE_STORE_FILE \
    RELEASE_STORE_PASSWORD \
    RELEASE_KEY_ALIAS \
    RELEASE_KEY_PASSWORD \

load_dotenv

run keytool -genkeypair \
  -keystore $RELEASE_STORE_FILE \
  -storetype PKCS12 \
  -storepass:env RELEASE_STORE_PASSWORD \
  -keypass:env RELEASE_KEY_PASSWORD \
  -alias $RELEASE_KEY_ALIAS \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=$AUTHOR_NAME, OU=$AUTHOR_ROLE, O=$AUTHOR_COMPANY, L=$AUTHOR_CITY, ST=$AUTHOR_STATE, C=$AUTHOR_COUNTRY"

install_system_dependencies

echo ""
echo "🚀 Creating Android Kotlin project: $PROJECT_NAME"
echo "📦 Package: $PACKAGE_NAME"
echo ""

# Create project root
mkdir -p "$PROJECT_NAME"
cd "$PROJECT_NAME"

# Initialize Git only if not already in a repo
if in_git_repo; then
    echo "📁 Detected existing Git repository. Skipping 'git init'."
else
    echo "Initializing new Git repository..."
    git init -q
fi

# --- Project Structure ---
echo "🏗️  Building project structure..."

mkdir -p app/src/main/java
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml

generateGradleWrapper() {

    which gradle || run sudo apt install gradle # this install a very old version
    run gradle --version
    run gradle wrapper --gradle-version 8.14.4 # update to a decent (supporting latest as version)
    run ./gradlew --version
    #run ./gradlew wrapper --gradle-version latest # update to the latest
    #run ./gradlew --version
}

# Create minimal settings.gradle to allow 'wrapper' task
cat > settings.gradle <<EOF
rootProject.name = '$PROJECT_NAME'

EOF

generateGradleWrapper

cat > gradle.properties <<EOF
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
kotlin.daemon.jvmargs=-Xmx2g
EOF

# --- Root build.gradle ---
cat > build.gradle.kts <<EOF
// Project level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
    id("com.github.ben-manes.versions") version "0.53.0"
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
EOF

cat > version.properties <<EOF
versionName=0.0.1
versionCode=00000100
EOF

# --- settings.gradle (final) ---
cat > settings.gradle <<EOF
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    //includeBuild("build-logic/plugins/android-yaml-strings")
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = '$PROJECT_NAME'
include ':app'
EOF

# --- app/build.gradle.kts ---
cat > app/build.gradle.kts <<EOF
// App module build file
import java.util.Properties
import java.io.FileInputStream
import com.android.build.api.variant.ApkOutput
import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("net.canvoki.android-yaml-strings") version "1.0.0"
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
            val key = parts[0].trim()
            val value = parts[1].trim()
            // Elimina cometes dobles o simples envolvents
            val cleanedValue = value.removeSurrounding("\"").removeSurrounding("'")
            project.ext.set(key, cleanedValue)
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

    namespace = "$PACKAGE_NAME"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "$PACKAGE_NAME"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = versionProps.getProperty("versionCode").toInt()
        versionName = versionProps.getProperty("versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Rename apks adding appid and version
    applicationVariants.all {
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                val version = mergedFlavor.versionName
                val buildType = buildType.name
                val appId = applicationId
                val flavorInfix = if (android.productFlavors.size > 1) "-\$flavorName" else ""
                outputFileName = "\${appId}\${flavorInfix}-\${version}-\${buildType}.apk"
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
        sourceCompatibility = JavaVersion.VERSION_$JAVA_VERSION
        targetCompatibility = JavaVersion.VERSION_$JAVA_VERSION
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_$JAVA_VERSION)
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
        unitTests {
            isReturnDefaultValues = true
        }
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
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
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
    // Platform BOM imports
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.kotlinx.coroutines.bom))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Tes
    testImplementation(libs.junit)
    testImplementation(libs.java.diff.utils)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
EOF

mkdir -p gradle
cat > gradle/libs.versions.toml <<EOF
[versions]
# Android sdk
compileSdk  = "$TARGET_SDK"
targetSdk = "$TARGET_SDK"
minSdk = "$MIN_SDK"

# Plugins
agp = "8.13.1"
kotlin = "2.3.10"
compose-compiler = "1.5.15"
spotless = "8.1.0"

# Libraries
androidx-activity-compose = "1.12.4"
androidx-appcompat = "1.7.1"
androidx-compose-bom = "2026.02.00"
androidx-constraintlayout = "2.2.1"
androidx-core-ktx = "1.17.0"
androidx-lifecycle-runtime-ktx = "2.10.0"
androidx-material = "1.13.0"
androidx-preference-ktx = "1.2.1"
androidx-test-core = "1.7.0"
androidx-test-ext-junit = "1.3.0"
androidx-test-espresso-core = "3.7.0"
androidx-test-runner = "1.6.2"
kotlinx-coroutines-bom = "1.10.2"


# Testing
junit = "4.13.2"
mockk = "1.14.6"
robolectric = "4.16"
java-diff-utils = "4.12"

[libraries]
# AndroidX Compose BOM
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose-bom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# Kotlinx Coroutines BOM
kotlinx-coroutines-bom = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-bom", version.ref = "kotlinx-coroutines-bom" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test" }

# AndroidX (no-BOM)
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidx-core-ktx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "androidx-appcompat" }
androidx-material = { group = "com.google.android.material", name = "material", version.ref = "androidx-material" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidx-activity-compose" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "androidx-constraintlayout" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "androidx-lifecycle-runtime-ktx" }
androidx-preference-ktx = { group = "androidx.preference", name = "preference-ktx", version.ref = "androidx-preference-ktx" }

# AndroidX Test
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidx-test-core" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext-junit" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidx-test-runner" }

# Testing libs
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }
java-diff-utils = { group = "io.github.java-diff-utils", name = "java-diff-utils", version.ref = "java-diff-utils" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
EOF



# --- AndroidManifest.xml ---
cat > app/src/main/AndroidManifest.xml <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <!--
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <queries>
        <intent>
            <action android:name="android.settings.LOCATION_SOURCE_SETTINGS" />
        </intent>
    </queries>
    -->
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.$PROJECT_NAME"
        tools:targetApi="$TARGET_SDK">

        <!-- Android 12+ themed icon declaration -->
        <!--
        <meta-data
            android:name="android.graphics.drawable.themedIcon"
            android:resource="@mipmap/ic_launcher_themed" />
        -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
EOF

# --- Create package directories ---
PACKAGE_DIR=$(echo "$PACKAGE_NAME" | tr '.' '/')
mkdir -p "app/src/main/kotlin/$PACKAGE_DIR"

# --- MainActivity.kt ---
cat > "app/src/main/kotlin/$PACKAGE_DIR/MainActivity.kt" <<EOF
package $PACKAGE_NAME

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
EOF

# --- Layout: activity_main.xml ---
cat > app/src/main/res/layout/activity_main.xml <<EOF
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
EOF

# --- strings.xml ---
mkdir -p app/src/main/res/values
cat > app/src/main/res/values/strings.xml <<EOF
<resources>
    <string name="app_name">$PROJECT_NAME</string>
</resources>
EOF

# --- themes.xml ---
cat > app/src/main/res/values/themes.xml <<EOF
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme -->
    <style name="Theme.$PROJECT_NAME" parent="Theme.Material3.DayNight">
        <!-- Customize your theme here -->
    </style>
</resources>
EOF

mkdir -p app/src/main/res/xml

# --- backup_rules.xml (API 23+) & data_extraction_rules.xml (API 31+) ---

cat > app/src/main/res/xml/backup_rules.xml <<EOF
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude files or folders -->
</full-backup-content>
EOF

cat > app/src/main/res/xml/data_extraction_rules.xml <<EOF
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <!-- Exclude files or folders -->
    </cloud-backup>
    <device-transfer>
        <!-- Exclude files or folders -->
    </device-transfer>
</data-extraction-rules>
EOF

echo "🖼️  Generating placeholder launcher icons..."

mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# Only generate if 'convert' is available
if command -v convert >/dev/null; then
    for dir in app/src/main/res/mipmap-*dpi; do
        convert -size 48x48 xc:blue -gravity center -fill white -pointsize 24 -annotate 0 "App" "$dir/ic_launcher.png" 2>/dev/null || true
        convert -size 48x48 xc:purple -gravity center -fill white -pointsize 24 -annotate 0 "App" "$dir/ic_launcher_round.png" 2>/dev/null || true
    done
    echo "   → ✅ Placeholder icons generated with ImageMagick."
else
    echo "   → ⚠️ 'convert' not found. Install 'imagemagick' for auto-generated icons."
    echo "   → You must add app/src/main/res/mipmap-*/ic_launcher.png manually."
fi

# --- .gitignore ---
cat > .gitignore <<EOF
.gradle/
build/
local.properties
*.iml
.idea/
*.apk
*.aar
captures/
app/release/
app/debug/
*.p12
.env
EOF

# --- Add all files to Git ---
echo "💾 Adding generated files to Git..."
git add .
git status --short 2>/dev/null || echo "✅ All project files staged for commit."

echo ""
echo "✅ Project '$PROJECT_NAME' created successfully!"
echo ""

if in_git_repo; then
    echo "ℹ️  Files added to existing Git repository."
else
    echo "🎉 New Git repository initialized and files added."
fi



echo ""
echo "📌 Next steps:"
echo "  1. Ensure Android SDK is installed:"
echo "     - Download Command-line Tools from https://developer.android.com/studio#command-tools"
echo "     - Extract to e.g., \$HOME/Android/cmdline-tools"
echo "     - Set ANDROID_HOME: export ANDROID_HOME=\$HOME/Android"
echo "     - Add to PATH: export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin"
echo "  2. Accept licenses: sdkmanager --licenses"
echo "  3. Build: ./gradlew assembleDebug"
echo "  4. Install on device: ./gradlew installDebug"
echo ""
echo "✅ This project uses Gradle Wrapper 8.7 — fully compatible with Java 21."
echo "📂 Project root: $(pwd)"
echo "📚 You can now open in Android Studio if desired — but not required!"
