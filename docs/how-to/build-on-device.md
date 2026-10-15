---
title: Build Termux:Access on-device in Termux
description: How-to for setting up an Android SDK/Gradle environment inside Termux and producing a debug APK without a PC.
---

# Build Termux:Access on-device in Termux

Use this when you want to compile the debug APK directly on the phone or tablet. No root and no separate computer are required.

## Prerequisites

- Termux (F-Droid or official GitHub build).
- Several GB of free storage.
- Stable internet connection for the first run (SDK and Gradle caches are large).

## Steps

1. Copy or clone the script.
2. Make it executable:
   ```bash
   chmod +x build-termux-access-ondevice.sh
   ```
3. Run it:
   ```bash
   ./build-termux-access-ondevice.sh
   ```
4. The script installs base packages, clones and runs the `termux-android-studio` installer, clones/updates `termux-access`, generates a Gradle wrapper if needed, and runs `assembleDebug`.
5. On success the APK is copied to `$HOME/termux-access-debug.apk`.

## After the build

Install locally:
```bash
termux-open ~/termux-access-debug.apk
```
or via ADB to another device:
```bash
adb install -r ~/termux-access-debug.apk
```

## Troubleshooting

- aapt2 architecture errors: the installer and the `android.aapt2FromMavenOverride` line address the common ARM64 issue. Re-run the installer if needed.
- Missing licenses: accept them with `yes | sdkmanager --licenses` after the SDK is present.
- Out of space: free several GB before re-running.
- First build is slow; subsequent builds are faster.
