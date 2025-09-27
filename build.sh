#!/bin/bash

export ANDROID_HOME="/usr/lib/android-sdk"

run() {
    echo -e "\033[34m:: $@\033[0m"
    "$@"
}

run ./gradlew assembleDebug &&
    run ./gradlew testDebugUnitTest &&
    (run adb uninstall net.canvoki.carburoid || true ) &&
    run ./gradlew installDebug &&
    run adb shell am start -n net.canvoki.carburoid/.MainActivity &&
    echo done


# adb shell cmd uimode night no
# adb shell cmd uimode night yes
# ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.canvoki.carburoid.LoadBenchmarkTest
