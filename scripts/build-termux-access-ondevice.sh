#!/data/data/com.termux/files/usr/bin/bash
# DLitFunc: Sets up an on-device Android build environment in Termux (via termux-android-studio),
# clones or updates termux-access, generates a Gradle wrapper if missing, runs assembleDebug,
# and copies the resulting APK to $HOME.
# Side effects: large network downloads (SDK, Gradle caches) only when missing, package installs, git clone/pull, Gradle build.
# Inputs: free storage (several GB), network. No root required.
# Outputs: termux-access-debug.apk in $HOME on success; exits non-zero on failure.
#           Full build log written to $HOME/termux-access.<YYYYMMDD-HHMMSS>.log.
set -euo pipefail

echo "=== On-device Termux:Access build automation ==="

termux-setup-storage || true

echo "Updating package lists..."
pkg update -y

# Install Termux-packaged tools only if missing (these stay in the local pkg cache after first install)
REQUIRED_PKGS=(git curl wget unzip openjdk-17 openjdk-21 gradle aapt2 android-tools)
MISSING=()
for p in "${REQUIRED_PKGS[@]}"; do
  if ! dpkg -s "$p" >/dev/null 2>&1; then
    MISSING+=("$p")
  fi
done
if [ ${#MISSING[@]} -gt 0 ]; then
  echo "Installing missing packages: ${MISSING[*]}"
  pkg install -y "${MISSING[@]}" || pkg install -y openjdk-17 gradle aapt2 git curl wget unzip android-tools
else
  echo "All required Termux packages already installed."
fi

STUDIO_DIR="$HOME/termux-studio"
if [ ! -d "$STUDIO_DIR" ]; then
  echo "Cloning termux-android-studio..."
  git clone https://github.com/poordevcode/termux-android-studio.git "$STUDIO_DIR"
else
  echo "termux-android-studio already present."
fi

# Only run the full installer if the SDK is missing; otherwise pass --no-sdk
SDK_FLAG=""
if [ -d "$HOME/android-sdk" ] && [ -x "$HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Android SDK already present; skipping SDK download."
  SDK_FLAG="--no-sdk"
else
  echo "Android SDK not found; running full installer (downloads SDK)."
fi

echo "Running termux-android-studio installer..."
bash "$STUDIO_DIR/install.sh" -y $SDK_FLAG || {
  echo "Installer reported an issue. Continuing — re-run if needed."
}

if [ -f "$HOME/.bashrc" ]; then
  # shellcheck disable=SC1090
  source "$HOME/.bashrc" || true
fi

# Robust JAVA_HOME for Termux (prefers java-21, falls back to java-17 or whatever is present)
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "${JAVA_HOME}" ]; then
  if [ -d "$PREFIX/lib/jvm/java-21-openjdk" ]; then
    export JAVA_HOME="$PREFIX/lib/jvm/java-21-openjdk"
  elif [ -d "$PREFIX/lib/jvm/java-17-openjdk" ]; then
    export JAVA_HOME="$PREFIX/lib/jvm/java-17-openjdk"
  else
    # Last resort: derive from `java` binary
    JAVA_BIN=$(command -v java || true)
    if [ -n "$JAVA_BIN" ]; then
      export JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")
    fi
  fi
fi
echo "Using JAVA_HOME=$JAVA_HOME"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
mkdir -p "$HOME/.gradle"
if ! grep -q "aapt2FromMavenOverride" "$HOME/.gradle/gradle.properties" 2>/dev/null; then
  echo "android.aapt2FromMavenOverride=${PREFIX}/bin/aapt2" >> "$HOME/.gradle/gradle.properties" || true
fi

REPO_DIR="$HOME/termux-access"
if [ ! -d "$REPO_DIR" ]; then
  echo "Cloning termux-access..."
  git clone https://github.com/landNull/termux-access.git "$REPO_DIR"
else
  echo "Updating existing termux-access clone..."
  cd "$REPO_DIR"
  # Stash any local changes so pull can succeed, then re-apply
  git stash push -u -m "auto-stash before pull" || true
  git pull --ff-only || git pull --rebase || true
  git stash pop || true
fi

cd "$REPO_DIR"

# Ensure wrapper uses Gradle 8.9+ (required by AGP 8.7)
if [ ! -f "./gradlew" ] || ! grep -q "gradle-8.9" gradle/wrapper/gradle-wrapper.properties 2>/dev/null; then
  echo "Generating / updating Gradle wrapper to 8.9..."
  if command -v gradle >/dev/null 2>&1; then
    gradle wrapper --gradle-version 8.9 || true
    chmod +x gradlew 2>/dev/null || true
  fi
fi

# Timestamped log so each run is preserved
STAMP=$(date +%Y%m%d-%H%M%S)
LOG="$HOME/termux-access.${STAMP}.log"
echo ""
echo "=== Starting debug build (first run may take several minutes) ==="
echo "Full log: $LOG"

# Run the build and tee all output to the timestamped log file
if [ -x "./gradlew" ]; then
  ./gradlew assembleDebug --stacktrace --no-daemon 2>&1 | tee "$LOG" || {
    echo "gradlew assembleDebug failed. See $LOG"
    exit 1
  }
elif command -v gradle >/dev/null 2>&1; then
  gradle assembleDebug --stacktrace --no-daemon 2>&1 | tee "$LOG" || {
    echo "gradle assembleDebug failed. See $LOG"
    exit 1
  }
else
  echo "Neither ./gradlew nor gradle is available. Re-run after installer finishes."
  exit 1
fi

APK=$(find app/build/outputs/apk -name "*.apk" -type f 2>/dev/null | head -n 1 || true)
if [ -n "$APK" ] && [ -f "$APK" ]; then
  echo ""
  echo "=== BUILD SUCCEEDED ==="
  echo "APK: $APK"
  cp -v "$APK" "$HOME/termux-access-debug.apk" || true
  echo "Copied to: $HOME/termux-access-debug.apk"
  echo "Install with: adb install -r \"$APK\" or termux-open \"$APK\""
  echo "Build log: $LOG"
else
  echo "Build finished but no APK found under app/build/outputs/apk. See $LOG"
  exit 1
fi
