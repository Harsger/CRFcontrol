#!/bin/bash

set -e

PROJECT="CRFcontrol"

export ANDROID_HOME="../android-sdk"
TOOLS_DIR="/build-tools/25.0.2"

AAPT=${ANDROID_HOME}${TOOLS_DIR}"/aapt"
DX=${ANDROID_HOME}${TOOLS_DIR}"/dx"
ZIPALIGN=${ANDROID_HOME}${TOOLS_DIR}"/zipalign"
APKSIGNER=${ANDROID_HOME}${TOOLS_DIR}"/apksigner" 
PLATFORM=${ANDROID_HOME}"/platforms/android-25/android.jar"
ADB=${ANDROID_HOME}"/platform-tools/adb"

echo " cleaning "
rm -rf obj/*
rm -rf src/com/${PROJECT}/R.java

echo " generating R.java file"
$AAPT package -f -m -J src -M AndroidManifest.xml -S res -I $PLATFORM

echo " compiling "
javac -d obj -classpath "src:libs/jsch-0.1.55.jar" -bootclasspath $PLATFORM -source 1.7 -target 1.7 src/com/${PROJECT}/MainActivity.java
javac -d obj -classpath "src:libs/jsch-0.1.55.jar" -bootclasspath $PLATFORM -source 1.7 -target 1.7 src/com/${PROJECT}/R.java

echo " translating in Dalvik bytecode "
$DX --dex --output=classes.dex libs/*.jar obj

echo " making APK "
$AAPT package -f -m -F bin/${PROJECT}.unaligned.apk -M AndroidManifest.xml -S res -I $PLATFORM
$AAPT add bin/${PROJECT}.unaligned.apk classes.dex

echo " aligning APK "
$ZIPALIGN -f 4 bin/${PROJECT}.unaligned.apk bin/${PROJECT}.apk

echo " signing APK "
$APKSIGNER sign --ks mykey.keystore bin/${PROJECT}.apk

if [ "$1" == "I" ]; then
    echo " installing "
    ${ADB} install -r bin/${PROJECT}.apk
    ${ADB} shell am start -n com.${PROJECT}/.MainActivity
fi
