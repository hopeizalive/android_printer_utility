#!/usr/bin/env bash
set -euo pipefail

# Download and install Android SDK command-line tools and required packages.
# Adjust SDK_ROOT if you want a different local SDK location.
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/home/codespace/Android/Sdk}}"

echo "Using SDK_ROOT=${SDK_ROOT}"
mkdir -p "$SDK_ROOT/cmdline-tools"
mkdir -p "$SDK_ROOT/licenses"

COMMANDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
TMP_ZIP="/tmp/commandlinetools-linux.zip"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to download the SDK tools."
  exit 1
fi
if ! command -v unzip >/dev/null 2>&1; then
  echo "unzip is required to extract the SDK tools."
  exit 1
fi

echo "Downloading Android SDK command-line tools..."
curl -L -f -o "$TMP_ZIP" "$COMMANDLINE_URL"

echo "Extracting command-line tools..."
unzip -q -o "$TMP_ZIP" -d "$SDK_ROOT/cmdline-tools"
rm -f "$TMP_ZIP"

if [ -d "$SDK_ROOT/cmdline-tools/cmdline-tools" ]; then
  mv -f "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
fi

SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  echo "sdkmanager is not found or executable: $SDKMANAGER"
  exit 1
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

PACKAGES=(
  "platform-tools"
  "platforms;android-35"
  "build-tools;35.0.0"
)

echo "Installing Android SDK packages: ${PACKAGES[*]}"
yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --install "${PACKAGES[@]}"

echo "Writing local.properties"
cat > local.properties <<EOF
sdk.dir=$SDK_ROOT
EOF

echo "Android SDK installation complete."
echo "SDK_ROOT=$SDK_ROOT"
echo "local.properties created at $(pwd)/local.properties"
