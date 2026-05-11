#!/usr/bin/env bash
set -euo pipefail

# Build the Android app in signed release mode.
# Uses the Gradle wrapper if present, otherwise falls back to the system gradle command.

if [ -f "./gradlew" ]; then
  GRADLE_CMD="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD="gradle"
else
  echo "No Gradle wrapper or system Gradle installation found."
  exit 1
fi

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$(pwd)/sdk}}"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"

if [ ! -f local.properties ]; then
  echo "Creating local.properties with sdk.dir=$SDK_ROOT"
  cat > local.properties <<EOF
sdk.dir=$SDK_ROOT
EOF
fi

if [ ! -f app/keystore.jks ]; then
  echo "Generating keystore for signing..."
  keytool -genkeypair -v -keystore app/keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias androiddebugkey -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
fi

echo "Building signed release APK with $GRADLE_CMD"
"$GRADLE_CMD" clean assembleRelease --continue

echo "Build finished. Signed release APK should be in app/build/outputs/apk/release/"
