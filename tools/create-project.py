#!/usr/bin/env python3
"""Generate minimal Android Kotlin project structure from CLI."""

import os
import shutil
import subprocess
import sys
from pathlib import Path
from string import Template
from typing import Optional

try:
    import typer
    from dotenv import dotenv_values, set_key
    from bs4 import BeautifulSoup
    import requests
except ImportError:
    print(
        """\
Either you are not in the Python environment or you didn't install the dependencies.
If you already have a user writable Python environment, install the dependencies with:
$ pip install typer python-dotenv beautfulsoup4 requests
"""
    )
    raise

# Project defaults
# https://developer.android.com/build/releases/agp-8-13-0-release-notes
TARGET_SDK = 36
MIN_SDK = 26
JAVA_VERSION = 17
GRADLE_VERSION = "8.14.4"
BUILD_TOOLS_VERSION = "35.0.0"
CMDLINE_TOOLS_VERSION = "19.0"
NDK_VERSION = "r26c"
AGP_VERSION = "8.13.2"
DEFAULT_ICON_BG = "#eeeeee"
DEFAULT_ICON_FG = "#2196F3"

# Color codes for CLI output
COLOR_BLUE = "\033[34m"
COLOR_YELLOW = "\033[33m"
COLOR_GREEN = "\033[32m"
COLOR_RED = "\033[31m"
COLOR_RESET = "\033[0m"


def success(message: str) -> None:
    """Print success message in green."""
    print(f"{COLOR_GREEN}✅ {message}{COLOR_RESET}")


def warn(message: str) -> None:
    """Print warning message in yellow."""
    print(f"{COLOR_YELLOW}⚠️  {message}{COLOR_RESET}")


def error(message: str) -> None:
    """Print error message in red."""
    print(f"{COLOR_RED}❌ {message}{COLOR_RESET}")


def fail(message: str) -> None:
    """Print error message and exit with code 1."""
    error(message)
    raise typer.Exit(1)


def step(message: str) -> None:
    """Print step message in blue."""
    print(f"{COLOR_BLUE}:: {message}{COLOR_RESET}")


def run(*args: str, check: bool = True, **kwargs) -> subprocess.CompletedProcess:
    """Execute command and print it in blue. Raise on error if check=True."""
    step(" ".join(args))
    result = subprocess.run(args, check=False, capture_output=False, **kwargs)
    if check and result.returncode != 0:
        raise subprocess.CalledProcessError(result.returncode, args)
    return result


def norun(*args: str) -> None:
    """Print command as skipped in yellow."""
    print(f"Skipped: {COLOR_YELLOW}:: {' '.join(args)}{COLOR_RESET}")


def write_content(path: Path, content: str, **substitutions: str) -> None:
    """Write content to file, applying template substitutions.

    Raises KeyError if a template variable is not provided.
    Use $$ to escape a literal $.
    """
    step(f"Writing {path}...")
    rendered = Template(content).substitute(substitutions)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(rendered, encoding="utf-8")


def load_dotenv(dotenv_path: Path) -> dict[str, str]:
    """Load environment variables from .env file."""
    if dotenv_path.exists():
        return dotenv_values(dotenv_path)
    return {}


def update_dotenv(dotenv_path: Path, **updates: str) -> None:
    """Update .env file with new or existing variables."""
    dotenv_path.touch(exist_ok=True)
    for key, value in updates.items():
        set_key(str(dotenv_path), key, value)


def package_to_name(package: str) -> str:
    """Convert package name to project name (capitalize last segment)."""
    return package.split(".")[-1].capitalize()


def in_git_repo(directory: Path) -> bool:
    """Check if directory is inside a Git repository."""
    result = subprocess.run(
        ["git", "rev-parse", "--git-dir"],
        cwd=directory,
        capture_output=True,
        check=False,
    )
    return result.returncode == 0


def fetch_gradle_checksum(version: str) -> str | None:
    """Fetch SHA-256 checksum for Gradle -bin.zip from official site."""
    url = "https://gradle.org/release-checksums/"
    response = requests.get(url, timeout=30)
    response.raise_for_status()

    soup = BeautifulSoup(response.text, "html.parser")

    # Find the anchor for this version
    anchor = soup.find("a", id=version) or soup.find("a", name=version)
    if not anchor:
        return None

    # Find the next <ul> containing checksums
    ul = anchor.find_next("ul")
    if not ul:
        return None

    # Look for the "Binary-only (-bin) ZIP Checksum" entry
    for li in ul.find_all("li"):
        text = li.get_text()
        if "Binary-only" in text and "-bin" in text:
            code = li.find("code")
            if code:
                checksum = code.get_text().strip()
                # Validate SHA-256 format (64 hex characters)
                if len(checksum) == 64 and all(c in "0123456789abcdef" for c in checksum.lower()):
                    return checksum

    fail(f"Checkum not found for gradle version {gradle_version}")

def generate_gradle_wrapper(project_root: Path, gradle_version: str) -> None:
    """Generate Gradle wrapper files."""
    if not shutil.which("gradle"):
        run("sudo", "apt", "install", "gradle")
    run("gradle", "--version")
    run("gradle", "wrapper", f"--gradle-version={gradle_version}", cwd=project_root)
    run("./gradlew", "--version", cwd=project_root)

    step(f"Fetching Gradle {gradle_version} checksum for F-Droid...")
    checksum = fetch_gradle_checksum(gradle_version)

    wrapper_props = project_root / "gradle/wrapper/gradle-wrapper.properties"
    content = wrapper_props.read_text(encoding="utf-8")

    if "gradleWrapperSha256Sum" not in content:
        content = content.rstrip() + f"\ngradleWrapperSha256Sum={checksum}\n"
        wrapper_props.write_text(content, encoding="utf-8")
        success(f"Added gradleWrapperSha256Sum to gradle-wrapper.properties")


def generate_project_structure(project_root: Path, package_name: str) -> None:
    """Create base directory structure."""
    dirs = [
        project_root / "app/src/main/kotlin",
        project_root / "app/src/main/res/layout",
        project_root / "app/src/main/res/values",
        project_root / "app/src/main/res/xml",
    ]
    for d in dirs:
        d.mkdir(parents=True, exist_ok=True)


def generate_gradle_files(
    project_root: Path, project_name: str, package_name: str
) -> None:
    """Generate root Gradle configuration files."""
    settings_gradle = """\
rootProject.name = '$PROJECT_NAME'
"""
    write_content(
        project_root / "settings.gradle", settings_gradle, PROJECT_NAME=project_name
    )

    gradle_properties = """\
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
kotlin.daemon.jvmargs=-Xmx2g
"""
    write_content(project_root / "gradle.properties", gradle_properties)

    build_gradle_kts = """\
// Project level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
    id("com.github.ben-manes.versions") version "0.53.0"
}

// Dependency updates configuration: reject unstable versions
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.matches(Regex(".*[.-](alpha|beta|rc|dev|snapshot|eap).*", RegexOption.IGNORE_CASE))
    }
    outputFormatter = "json,plain"
    checkForGradleUpdate = true
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
"""
    write_content(project_root / "build.gradle.kts", build_gradle_kts)

    version_properties = """\
versionName=0.0.1
versionCode=00000100
"""
    write_content(project_root / "version.properties", version_properties)

    settings_gradle_final = """\
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
"""
    write_content(
        project_root / "settings.gradle", settings_gradle_final, PROJECT_NAME=project_name
    )


def generate_app_build_gradle(
    project_root: Path, package_name: str, java_version: str
) -> None:
    """Generate app module build.gradle.kts."""
    content = """\
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
            // Removes quotes
            val cleanedValue = value.removeSurrounding("\\"").removeSurrounding("'")
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
                val flavorInfix = if (android.productFlavors.size > 1) "-$$flavorName" else ""
                outputFileName = "$${appId}$${flavorInfix}-$${version}-$${buildType}.apk"
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
            isMinifyEnabled = true
            isShrinkResources = true
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
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
"""
    write_content(
        project_root / "app" / "build.gradle.kts",
        content,
        PACKAGE_NAME=package_name,
        JAVA_VERSION=java_version,
    )


def generate_version_catalog(
    project_root: Path, target_sdk: int, min_sdk: int, agp_version: str,
) -> None:
    """Generate gradle/libs.versions.toml."""
    content = f"""\
[versions]
# Android sdk
compileSdk  = "{target_sdk}"
targetSdk = "{target_sdk}"
minSdk = "{min_sdk}"

# Plugins
agp = "{agp_version}"
kotlin = "2.3.20"
compose-compiler = "1.5.15"
spotless = "8.4.0"

# Libraries
androidx-activity-compose = "1.13.0"
androidx-appcompat = "1.7.1"
androidx-compose-bom = "2026.03.01"
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

[libraries]
# AndroidX Compose BOM
androidx-compose-bom = {{ group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose-bom" }}
androidx-compose-material3 = {{ group = "androidx.compose.material3", name = "material3" }}
androidx-compose-ui = {{ group = "androidx.compose.ui", name = "ui" }}
androidx-compose-ui-tooling = {{ group = "androidx.compose.ui", name = "ui-tooling" }}
androidx-compose-ui-tooling-preview = {{ group = "androidx.compose.ui", name = "ui-tooling-preview" }}

# Kotlinx Coroutines BOM
kotlinx-coroutines-bom = {{ group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-bom", version.ref = "kotlinx-coroutines-bom" }}
kotlinx-coroutines-android = {{ group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines-bom" }}
kotlinx-coroutines-test = {{ group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines-bom" }}

# AndroidX (no-BOM)
androidx-core-ktx = {{ group = "androidx.core", name = "core-ktx", version.ref = "androidx-core-ktx" }}
androidx-appcompat = {{ group = "androidx.appcompat", name = "appcompat", version.ref = "androidx-appcompat" }}
androidx-material = {{ group = "com.google.android.material", name = "material", version.ref = "androidx-material" }}
androidx-activity-compose = {{ group = "androidx.activity", name = "activity-compose", version.ref = "androidx-activity-compose" }}
androidx-constraintlayout = {{ group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "androidx-constraintlayout" }}
androidx-lifecycle-runtime-ktx = {{ group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "androidx-lifecycle-runtime-ktx" }}
androidx-preference-ktx = {{ group = "androidx.preference", name = "preference-ktx", version.ref = "androidx-preference-ktx" }}

# AndroidX Test
androidx-test-core = {{ group = "androidx.test", name = "core", version.ref = "androidx-test-core" }}
androidx-test-ext-junit = {{ group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext-junit" }}
androidx-test-runner = {{ group = "androidx.test", name = "runner", version.ref = "androidx-test-runner" }}

# Testing libs
junit = {{ group = "junit", name = "junit", version.ref = "junit" }}
kotlin-test = {{ group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }}

[plugins]
android-application = {{ id = "com.android.application", version.ref = "agp" }}
kotlin-android = {{ id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }}
kotlin-compose = {{ id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }}
spotless = {{ id = "com.diffplug.spotless", version.ref = "spotless" }}
"""
    write_content(project_root / "gradle/libs.versions.toml", content)


def generate_manifest(project_root: Path, project_name: str, target_sdk: int) -> None:
    """Generate AndroidManifest.xml."""

    content = f"""\
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
        android:theme="@style/Theme.{project_name}"
        tools:targetApi="{target_sdk}">

        <!-- Android 12+ themed icon declaration -->
        <meta-data
            android:name="android.graphics.drawable.themedIcon"
            android:resource="@mipmap/ic_launcher_themed" />

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
"""
    write_content(project_root / "app/src/main/AndroidManifest.xml", content)


def generate_compose_ui(project_root: Path, package_name: str) -> None:
    """Generate Compose UI components."""
    ui_dir = project_root / "app/src/main/kotlin" / package_name.replace(".", "/") / "ui"

    write_content(ui_dir / "BrandedText.kt", f"""\
package {package_name}.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BrandedText(
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier,
) {{
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {{
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.1f),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }}
}}
""")

    write_content(ui_dir / "AppScaffold.kt", f"""\
package {package_name}.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier


@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {{}},
    content: @Composable ColumnScope.() -> Unit,
) {{
    val isDark = isSystemInDarkTheme()
    val colorScheme = remember(isDark) {{
        if (isDark) darkColorScheme() else lightColorScheme()
    }}

    MaterialTheme(colorScheme = colorScheme) {{
        Scaffold(
            topBar = topBar,
            contentWindowInsets = WindowInsets.safeDrawing,
        ) {{ padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {{
                content()
            }}
        }}
    }}
}}
""")

def generate_kotlin_files(project_root: Path, package_name: str) -> None:
    """Generate Kotlin source files."""
    package_dir = package_name.replace(".", "/")
    main_activity_path = project_root / f"app/src/main/kotlin/{package_dir}/MainActivity.kt"
    main_activity = f"""\
package {package_name}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import {package_name}.ui.BrandedText
import {package_name}.ui.AppScaffold

class MainActivity : AppCompatActivity() {{
    override fun onCreate(savedInstanceState: Bundle?) {{
        super.onCreate(savedInstanceState)
        setContent {{
            AppScaffold {{
                BrandedText(
                    text = "Hello, World!",
                    icon = painterResource(R.drawable.ic_brand),
                    modifier = Modifier.fillMaxSize(),
                )
            }}
        }}
    }}
}}
"""

    write_content(
        main_activity_path,
        main_activity,
    )


def generate_resources(project_root: Path, project_name: str) -> None:
    """Generate resource files (strings, themes, xml rules)."""
    strings = f"""\
<resources>
    <string name="app_name">{project_name}</string>
</resources>
"""
    write_content(
        project_root / "app/src/main/res/values/strings.xml", strings
    )

    themes = f"""\
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme -->
    <style name="Theme.{project_name}" parent="Theme.Material3.DayNight">
        <!-- Customize your theme here -->
    </style>
</resources>
"""
    write_content(
        project_root / "app/src/main/res/values/themes.xml", themes
    )

    backup_rules = """\
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude files or folders -->
</full-backup-content>
"""
    write_content(
        project_root / "app/src/main/res/xml/backup_rules.xml",
        backup_rules,
    )

    data_extraction_rules = """\
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <!-- Exclude files or folders -->
    </cloud-backup>
    <device-transfer>
        <!-- Exclude files or folders -->
    </device-transfer>
</data-extraction-rules>
"""
    write_content(
        project_root / "app/src/main/res/xml/data_extraction_rules.xml",
        data_extraction_rules,
    )

def generate_placeholder_svg(
    output_path: Path,
    bg_color: str,
    fg_color: str,
) -> None:
    """Generate a placeholder SVG with a simple geometric figure as foreground."""
    # Simple rounded square / app-like icon path (24x24 viewBox, centered)
    # This is a generic "app" shape: rounded square with a small inner detail
    path_data = "M12 2L4 4C2.89543 4.27614 2 5.16157 2 6.26667V17.7333C2 18.8384 2.89543 19.7239 4 20L12 22L20 20C21.1046 19.7239 22 18.8384 22 17.7333V6.26667C22 5.16157 21.1046 4.27614 20 4L12 2Z"

    svg_content = f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg"
     width="24" height="24" viewBox="0 0 24 24">
  <path d="{path_data}" fill="{fg_color}"/>
</svg>'''

    write_content(output_path, svg_content)

def use_custom_svg(
    custom_path: Path,
    output_path: Path,
) -> None:
    """Copy custom SVG to project root as icon.svg."""
    if not custom_path.exists():
        fail(f"Custom icon SVG not found: {custom_path}")

    shutil.copy2(custom_path, output_path)
    step(f"Copied custom SVG: {custom_path} → {output_path}")

def generate_launcher_icon(
    project_root: Path,
    svg_path: Path,
    bg_color: str,
    fg_color: str,
    minsdk: int,
) -> None:
    """Generate adaptive launcher icons by calling app-icon-generator.py."""
    script_dir = Path(__file__).parent
    icon_generator = script_dir / "app-icon-generator.py"

    if not icon_generator.exists():
        fail(f"app-icon-generator.py not found at {icon_generator}")

    step(f"Generating adaptive icons from {svg_path}...")

    run(
        sys.executable,
        str(icon_generator),
        "launcher-icon",
        str(svg_path),
        "--bg", bg_color,
        "--fg", fg_color,
        "--out", str(project_root / "app/src/main/res"),
        "--minsdk", str(minsdk),
    )
    success("Adaptive icons generated.")

def download_material_icon_svg(
    icon_name: str,
    output_path: Path,
) -> None:
    """Download Material Icon SVG by delegating to material-icons-android-importer.py."""
    script_dir = Path(__file__).parent
    importer = script_dir / "material-icons-android-importer.py"

    run(
        sys.executable,
        str(importer),
        "download-icon",
        icon_name,
        "--output", str(output_path),
    )

def generate_icons(
    project_root: Path,
    bg_color: str,
    fg_color: str,
    icon: str,
    minsdk: int,
) -> None:
    """Generate adaptive launcher icons (placeholder or custom SVG + app-icon-generator)."""
    svg_path = project_root / "icon.svg"

    if icon == 'default':
        generate_placeholder_svg(svg_path, bg_color, fg_color)
    elif icon.endswith('.svg'):
        use_custom_svg(Path(icon), svg_path)
    else:
        download_material_icon_svg(icon, svg_path)

    generate_launcher_icon(project_root, svg_path, bg_color, fg_color, minsdk)

def generate_editorconfig(project_root: Path) -> None:
    """Generate .editorconfig with Compose-friendly ktlint rules."""
    write_content(project_root / ".editorconfig", """\
# .editorconfig
root = true

[*.{kt,kts}]
end_of_line = lf
indent_size = 4
indent_style = space
insert_final_newline = true
max_line_length = 120
ktlint_code_style = ktlint_official
#ktlint_code_style = android_studio
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
#ij_kotlin_imports_layout = *,java.**,javax.**,kotlin.**,^
#ij_kotlin_packages_to_use_import_on_demand = unset
#ktlint_function_signature_body_expression_wrapping = multiline
ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2
#ktlint_ignore_back_ticked_identifier = false
ktlint_standard_comment-spacing = disabled
ktlint_standard_value-argument-comment = disabled
ktlint_standard_type-parameter-comment = disabled
ktlint_standard_value-parameter-comment = disabled
ktlint_function_naming_ignore_when_annotated_with = Composable
""")

def generate_gitignore(project_root: Path) -> None:
    """Generate .gitignore file."""
    content = """\
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
"""
    write_content(project_root / ".gitignore", content)


def copy_config_files(project_root: Path, env_path: Path, keystore_path: Path) -> None:
    """Copy .env and keystore to project directory for build."""
    step("Copying configuration to project directory...")
    if env_path.exists():
        shutil.copy2(env_path, project_root / ".env")
        success(".env copied from parent directory")
    else:
        warn("No .env found in parent directory")

    if keystore_path.exists():
        shutil.copy2(keystore_path, project_root / keystore_path.name)
        success(f"Keystore copied: {keystore_path.name}")
    else:
        warn(f"Keystore not found at {keystore_path}")


def generate_keystore(
    *,
    release_store_file: Path,
    store_password: str,
    key_password: str,
    key_alias: str,
    author_name: str,
    author_role: str,
    author_company: str,
    author_city: str,
    author_state: str,
    author_country: str,
) -> None:
    """Generate PKCS12 keystore if it doesn't exist."""
    if release_store_file.exists():
        warn(f"Reusing existing keystore: {release_store_file}")
        return

    env = os.environ.copy()
    env["RELEASE_STORE_PASSWORD"] = store_password
    env["RELEASE_KEY_PASSWORD"] = key_password

    dname = f"CN={author_name}, OU={author_role}, O={author_company}, L={author_city}, ST={author_state}, C={author_country}"

    run(
        "keytool",
        "-genkeypair",
        "-keystore",
        str(release_store_file),
        "-storetype",
        "PKCS12",
        "-storepass:env",
        "RELEASE_STORE_PASSWORD",
        "-keypass:env",
        "RELEASE_KEY_PASSWORD",
        "-alias",
        key_alias,
        "-keyalg",
        "RSA",
        "-keysize",
        "4096",
        "-validity",
        "10000",
        "-dname",
        dname,
        env=env,
    )


def prompt_parameter(
    var_name: str,
    cli_value: Optional[str],
    prompt_text: str,
    default_value: str,
    env_vars: dict[str, str],
    is_password: bool = False,
) -> str:
    """Prompt for parameter value with CLI/env/default fallback."""
    if cli_value:
        step(f"{var_name} taken from command line: '{cli_value}'")
        return cli_value

    env_value = env_vars.get(var_name)
    if env_value:
        return env_value

    if is_password:
        import getpass
        return getpass.getpass(f"{prompt_text} ({var_name}): ")

    value = input(f"{prompt_text} ({var_name}) [{default_value}]: ").strip()
    return value if value else default_value


app = typer.Typer(add_completion=False)


@app.command()
def main(
    package_name: Optional[str] = typer.Argument(
        None, help="Android package name (e.g., com.example.app)"
    ),
    project_name: Optional[str] = typer.Argument(
        None, help="Project display name"
    ),
    release_store_file: Optional[str] = typer.Option(
        None, help="Path to release keystore file"
    ),
    release_store_password: Optional[str] = typer.Option(
        None, help="Keystore password", hide_input=True
    ),
    release_key_alias: Optional[str] = typer.Option(None, help="Key alias"),
    release_key_password: Optional[str] = typer.Option(
        None, help="Key password", hide_input=True
    ),
    author_name: Optional[str] = typer.Option(None, help="Author name"),
    author_company: Optional[str] = typer.Option(None, help="Author company"),
    author_role: Optional[str] = typer.Option(None, help="Author role"),
    author_city: Optional[str] = typer.Option(None, help="Author city"),
    author_state: Optional[str] = typer.Option(None, help="Author state"),
    author_country: Optional[str] = typer.Option(None, help="Author country ISO code"),
    icon: str = typer.Option(
        None,
        "--icon",
        help="Launcher icon: 'default' (generated), path to SVG file, or Material Icon ID",
    ),
    icon_bg: str = typer.Option(
        None,
        "--icon-bg",
        help="Launcher icon background color",
    ),
    icon_fg: str = typer.Option(
        None,
        "--icon-fg",
        help="Launcher icon foreground color",
    ),
):
    """Create a minimal Android Kotlin project structure from CLI."""
    # Load existing .env for defaults
    dotenv_path = Path.cwd() / ".env"
    env_vars = load_dotenv(dotenv_path)

    # Collect parameters
    pkg_name = prompt_parameter(
        var_name="PACKAGE_NAME",
        cli_value=package_name,
        prompt_text="Enter package name",
        default_value="com.example.dummy",
        env_vars=env_vars,
    )
    pkg_short = pkg_name.split(".")[-1]
    proj_name = prompt_parameter(
        var_name="PROJECT_NAME",
        cli_value=project_name,
        prompt_text="Enter project name",
        default_value=package_to_name(pkg_name),
        env_vars=env_vars,
    )
    store_file = Path(
        prompt_parameter(
            var_name="RELEASE_STORE_FILE",
            cli_value=release_store_file,
            prompt_text="Enter keystore path",
            default_value=str(Path.cwd() / f"{pkg_short}-release.p12"),
            env_vars=env_vars,
        )
    )
    store_pass = prompt_parameter(
        var_name="RELEASE_STORE_PASSWORD",
        cli_value=release_store_password,
        prompt_text="Enter key store password",
        default_value="",
        env_vars=env_vars,
        is_password=True,
    )
    key_alias = prompt_parameter(
        var_name="RELEASE_KEY_ALIAS",
        cli_value=release_key_alias,
        prompt_text="Enter key alias",
        default_value=pkg_short,
        env_vars=env_vars,
    )
    key_pass = prompt_parameter(
        var_name="RELEASE_KEY_PASSWORD",
        cli_value=release_key_password,
        prompt_text="Enter key password",
        default_value="",
        env_vars=env_vars,
        is_password=True,
    )
    auth_name = prompt_parameter(
        var_name="AUTHOR_NAME",
        cli_value=author_name,
        prompt_text="Enter your name",
        default_value=env_vars.get("AUTHOR_NAME", ""),
        env_vars=env_vars,
    )
    auth_company = prompt_parameter(
        var_name="AUTHOR_COMPANY",
        cli_value=author_company,
        prompt_text="Enter your company",
        default_value="",
        env_vars=env_vars,
    )
    auth_role = prompt_parameter(
        var_name="AUTHOR_ROLE",
        cli_value=author_role,
        prompt_text="Enter your role",
        default_value="",
        env_vars=env_vars,
    )
    auth_city = prompt_parameter(
        var_name="AUTHOR_CITY",
        cli_value=author_city,
        prompt_text="Enter your city",
        default_value="",
        env_vars=env_vars,
    )
    auth_state = prompt_parameter(
        var_name="AUTHOR_STATE",
        cli_value=author_state,
        prompt_text="Enter your state",
        default_value="",
        env_vars=env_vars,
    )
    auth_country = prompt_parameter(
        var_name="AUTHOR_COUNTRY",
        cli_value=author_country,
        prompt_text="Enter your country ISO code",
        default_value="",
        env_vars=env_vars,
    )
    icon = prompt_parameter(
        var_name="ICON",
        cli_value=icon,
        prompt_text="Enter base for application icon (default, svg file or a material-icon id)",
        default_value="default",
        env_vars=env_vars,
    )
    icon_fg = prompt_parameter(
        var_name="ICON_FG",
        cli_value=icon_fg,
        prompt_text="Enter application icon foreground color",
        default_value=DEFAULT_ICON_FG,
        env_vars=env_vars,
    )
    icon_bg = prompt_parameter(
        var_name="ICON_BG",
        cli_value=icon_bg,
        prompt_text="Enter application icon background color",
        default_value=DEFAULT_ICON_BG,
        env_vars=env_vars,
    )


    # Update .env with collected values
    update_dotenv(
        dotenv_path,
        AUTHOR_NAME=auth_name,
        AUTHOR_COMPANY=auth_company,
        AUTHOR_ROLE=auth_role,
        AUTHOR_CITY=auth_city,
        AUTHOR_STATE=auth_state,
        AUTHOR_COUNTRY=auth_country,
        RELEASE_STORE_FILE=str(store_file),
        RELEASE_STORE_PASSWORD=store_pass,
        RELEASE_KEY_ALIAS=key_alias,
        RELEASE_KEY_PASSWORD=key_pass,
        ICON=icon,
        ICON_FG=icon_fg,
        ICON_BG=icon_bg,
    )

    # Generate keystore
    generate_keystore(
        release_store_file=store_file,
        store_password=store_pass,
        key_password=key_pass,
        key_alias=key_alias,
        author_name=auth_name,
        author_role=auth_role,
        author_company=auth_company,
        author_city=auth_city,
        author_state=auth_state,
        author_country=auth_country,
    )

    # Install system dependencies (skipped by default)
    norun(
        "sudo",
        "apt",
        "install",
        "gradle",
        "google-android-emulator-installer",
        "google-android-platform-tools-installer",
        f"google-android-build-tools-{BUILD_TOOLS_VERSION}-installer",
        f"google-android-cmdline-tools-{CMDLINE_TOOLS_VERSION}-installer",
        f"google-android-ndk-{NDK_VERSION}-installer",
        f"google-android-platform-{TARGET_SDK}-installer",
    )

    # Create project
    step(f"Creating Android Kotlin project: {proj_name}")
    step(f"Package: {pkg_name}")

    project_root = Path.cwd() / proj_name.lower()
    project_root.mkdir(exist_ok=True)

    # Git initialization
    if in_git_repo(Path.cwd()):
        step("Detected existing Git repository. Skipping 'git init'.")
    else:
        run("git", "init", "-q", cwd=project_root)

    # Generate structure and files
    step("Building project structure...")
    generate_project_structure(project_root=project_root, package_name=pkg_name)
    generate_gradle_wrapper(project_root=project_root, gradle_version=GRADLE_VERSION)
    generate_gradle_files(
        project_root=project_root, project_name=proj_name, package_name=pkg_name
    )
    generate_app_build_gradle(
        project_root=project_root, package_name=pkg_name, java_version=str(JAVA_VERSION)
    )
    generate_version_catalog(
        project_root=project_root, target_sdk=TARGET_SDK, min_sdk=MIN_SDK, agp_version=AGP_VERSION,
    )
    generate_manifest(
        project_root=project_root, project_name=proj_name, target_sdk=TARGET_SDK
    )
    generate_kotlin_files(project_root=project_root, package_name=pkg_name)
    generate_compose_ui(project_root=project_root, package_name=pkg_name)
    generate_resources(project_root=project_root, project_name=proj_name)
    generate_icons(
        project_root=project_root,
        bg_color=icon_bg,
        fg_color=icon_fg,
        icon=icon,
        minsdk=MIN_SDK,
    )
    generate_gitignore(project_root=project_root)
    generate_editorconfig(project_root=project_root)
    copy_config_files(
        project_root=project_root, env_path=dotenv_path, keystore_path=store_file
    )

    # Git staging
    step("Adding generated files to Git...")
    run("git", "add", ".", cwd=project_root, check=False)
    run("git", "status", "--short", cwd=project_root, check=False)

    success(f"Project '{proj_name}' created successfully!")

    if in_git_repo(Path.cwd()):
        step("Files added to existing Git repository.")
    else:
        success("New Git repository initialized and files added.")

    # Next steps
    print(
        f"""\n📌 Next steps:
  1. Ensure ANDROID HOME is exported properly.
  2. Accept licenses: sdkmanager --licenses
  3. Build: ./gradlew build
  4. Install on device: ./gradlew installFlossDebug
  5. Check dependency updates: ./gradlew dependencyUpdates

✅ This project uses Gradle Wrapper {GRADLE_VERSION} — fully compatible with Java {JAVA_VERSION}.
📂 Project root: {project_root}
📚 You can now open in Android Studio if desired — but not required!"""
    )


if __name__ == "__main__":
    app()
