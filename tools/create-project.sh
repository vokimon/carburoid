#!/bin/bash

# create-android-kotlin-project.sh
# Creates a minimal Android Kotlin project structure from CLI, no IDE.
# sudo apt install
# Uses downloaded Gradle 8.7 to generate wrapper — Java 21 compatible.
# Prompts for project/package using 3-arg prompt_with_default.
# Detects Git repo in current or parent directories.

set -e  # Exit on any error

run() {
    echo -e "\033[34m:: $@\033[0m"
    "$@"
}

install_system_dependencies() {
    run sudo apt install \
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


# --- Prompt with default — supports CLI override ---
prompt_with_default() {
    local prompt_text="$1"
    local default_value="$2"
    local cli_value="$3"

    if [ -n "$cli_value" ]; then
        echo "$cli_value"
        return
    fi

    read -p "$prompt_text [$default_value]: " user_input
    if [ -z "$user_input" ]; then
        echo "$default_value"
    else
        echo "$user_input"
    fi
}

# --- Detect if inside any Git repo (current or parent) ---
in_git_repo() {
    if git rev-parse --git-dir > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

install_system_dependencies

# --- Get project name and package name ---
PROJECT_NAME=$(prompt_with_default "Enter project name" "MyAndroidApp" "$1")
PACKAGE_NAME=$(prompt_with_default "Enter package name" "com.example.$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]')" "$2")

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
    run gradle wrapper --gradle-version 8.4 # update to a decent (supporting latest as version)
    run ./gradlew --version
    run ./gradlew wrapper --gradle-version latest # update to the latest
    run ./gradlew --version
}

# Create minimal settings.gradle to allow 'wrapper' task
cat > settings.gradle <<EOF
rootProject.name = '$PROJECT_NAME'

EOF

generateGradleWrapper

cat > gradle.properties <<EOF
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
EOF

# --- Root build.gradle ---
cat > build.gradle <<EOF
// Top-level build file
buildscript {
    ext.kotlin_version = '1.9.24'
    ext.agp_version = '8.4.0'

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:\$agp_version"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
EOF

# --- settings.gradle (final) ---
cat > settings.gradle <<EOF
rootProject.name = '$PROJECT_NAME'
include ':app'
EOF

# --- app/build.gradle ---
cat > app/build.gradle <<EOF
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace '$PACKAGE_NAME'
    compileSdk 34

    defaultConfig {
        applicationId "$PACKAGE_NAME"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
}
EOF

# --- AndroidManifest.xml ---
cat > app/src/main/AndroidManifest.xml <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.$PROJECT_NAME"
        tools:targetApi="31">

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
mkdir -p "app/src/main/java/$PACKAGE_DIR"

# --- MainActivity.kt ---
cat > "app/src/main/java/$PACKAGE_DIR/MainActivity.kt" <<EOF
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

# --- backup_rules.xml & data_extraction_rules.xml ---
mkdir -p app/src/main/res/xml

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
