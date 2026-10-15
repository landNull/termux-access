#!/data/data/com.termux/files/usr/bin/bash
# DLitFunc: Installs Termux packages, authenticates gh if needed, downloads the latest
# Termux:Access debug APK (via gh or nightly.link fallback), installs it on a connected
# ADB device, starts WebDavServerService, and streams filtered logs.
# Side effects: network downloads, package installs, adb push/install/start, logcat.
# Inputs: optional existing APK in $HOME or /sdcard/Download; ADB device already paired.
# Outputs: APK installed on device, service started, logcat running until Ctrl-C.
set -euo pipefail

echo "=== Termux:Access install + test via ADB ==="

termux-setup-storage || true

echo "Updating package lists..."
pkg update -y

REQUIRED_PKGS=(android-tools curl unzip git gh jq wget)
for pkg in "${REQUIRED_PKGS[@]}"; do
  if ! command -v "${pkg%%-*}" >/dev/null 2>&1 && \
     ! pkg list-installed 2>/dev/null | grep -q "^$pkg/"; then
    echo "Installing $pkg..."
    pkg install -y "$pkg"
  else
    echo "$pkg already present."
  fi
done

# Optional Git identity
if ! git config --global user.name >/dev/null 2>&1; then
  read -r -p "Git name (or Enter to skip): " GIT_NAME
  [ -n "$GIT_NAME" ] && git config --global user.name "$GIT_NAME"
fi
if ! git config --global user.email >/dev/null 2>&1; then
  read -r -p "Git email (or Enter to skip): " GIT_EMAIL
  [ -n "$GIT_EMAIL" ] && git config --global user.email "$GIT_EMAIL"
fi

echo ""
echo "=== GitHub CLI authentication ==="
if gh auth status >/dev/null 2>&1; then
  echo "Already authenticated."
else
  echo "GitHub CLI is not logged in."
  echo "Use device-code flow (complete at https://github.com/login/device on another device)."
  echo "Or set a token: echo YOUR_TOKEN | gh auth login --with-token"
  read -r -p "Press Enter to start interactive login (or Ctrl-C to abort and use token method)..."
  gh auth login || true
  if ! gh auth status >/dev/null 2>&1; then
    echo "Authentication incomplete. Finish with 'gh auth login' or token method, then re-run."
    exit 1
  fi
fi

APK_DIR="$HOME/termux-access-apk"
mkdir -p "$APK_DIR"
cd "$APK_DIR"

echo ""
echo "=== Downloading latest app-debug artifact ==="
if gh run download --repo landNull/termux-access --name app-debug --dir . 2>/dev/null; then
  echo "Downloaded via gh."
else
  echo "gh download failed. Trying nightly.link..."
  ARTIFACT_URL="https://nightly.link/landNull/termux-access/workflows/build/main/app-debug.zip"
  if curl -fL --retry 3 --retry-delay 2 -o app-debug.zip "$ARTIFACT_URL"; then
    unzip -o app-debug.zip
  else
    echo "Download failed. Place an APK in $HOME and re-run."
  fi
fi

APK=$(find . -name "*.apk" -type f | head -n 1 || true)
if [ -z "${APK:-}" ] || [ ! -f "$APK" ]; then
  for candidate in "$HOME/app-debug.apk" "$HOME/termux-access.apk" "/sdcard/Download/app-debug.apk"; do
    if [ -f "$candidate" ]; then
      APK="$candidate"
      break
    fi
  done
fi

if [ -z "${APK:-}" ] || [ ! -f "$APK" ]; then
  echo "No APK found. Build one on-device or download manually, then re-run."
  exit 1
fi
echo "Using APK: $APK"

echo ""
echo "Checking ADB devices..."
adb devices -l
if ! adb devices | grep -q "device$"; then
  echo "No device listed as 'device'. Pair wireless ADB first."
  exit 1
fi

echo "Pushing and installing..."
adb push "$APK" /data/local/tmp/termux-access.apk
adb shell pm install -r /data/local/tmp/termux-access.apk

echo "Starting WebDavServerService..."
adb shell am startservice -n com.termux.access/.WebDavServerService || \
  adb shell am start-foreground-service -n com.termux.access/.WebDavServerService || true

sleep 3

echo ""
echo "=== Service status ==="
adb shell dumpsys activity services com.termux.access | head -80

echo ""
echo "=== Next steps ==="
echo "Note the WebDAV port from the tablet notification."
echo "Test: curl -v -X PROPFIND http://<tablet-ip>:<port>/ -H 'Depth: 1'"
echo "Logs starting below (Ctrl-C to stop)."

adb logcat -c
adb logcat -s TermuxAccess:* WebDav:* *:E
