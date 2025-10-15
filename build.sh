#!/bin/bash

export ANDROID_HOME="/usr/lib/android-sdk"

run() {
    echo -e "\033[34m:: $@\033[0m"
    "$@"
}

run ./gradlew assembleDebug &&
    run ./gradlew test &&
    (run adb uninstall net.canvoki.carburoid || true ) &&
    run ./gradlew installFlossDebug &&
    run adb shell am start -n net.canvoki.carburoid/.MainActivity &&
    echo done


# adb shell cmd uimode night no
# adb shell cmd uimode night yes
# ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.canvoki.carburoid.LoadBenchmarkTest

# Generate the launcher icon
# curl https://material-icons.github.io/material-icons/svg/local_gas_station/baseline.svg > launcher_icon.svg
# python ../android-scripts/app-icon-generator.py launcher_icon.svg --out app/src/main/res/

# Change gradle version
# ./gradlew wrapper --gradle-version=8.14.3 --distribution-type=all
