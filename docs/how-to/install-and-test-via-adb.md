---
title: Install and test Termux:Access via ADB
description: How-to for using the phone-side script that downloads, installs, starts, and logs the app on a connected tablet.
---

# Install and test Termux:Access via ADB

Use this when you already have a wireless ADB connection from the phone to the tablet and want to install the latest debug APK, start the WebDAV service, and watch logs.

## Prerequisites

- Termux on the phone (F-Droid or official GitHub build preferred).
- Wireless debugging enabled on the tablet and the phone paired (`adb devices` shows the tablet as `device`).
- Internet access on the phone.

## Steps

1. Copy the script into Termux (or clone the repo and use the file from `scripts/`).
2. Make it executable:
   ```bash
   chmod +x install-and-test-via-adb.sh
   ```
3. Run it:
   ```bash
   ./install-and-test-via-adb.sh
   ```
4. If prompted, complete GitHub CLI authentication (device code at https://github.com/login/device or a personal access token).
5. The script downloads the latest `app-debug` artifact (or falls back to a local APK), pushes and installs it, starts `WebDavServerService`, prints service status, and begins filtered logcat.

## After the script

- Note the WebDAV port from the tablet notification.
- From the phone, test with:
  ```bash
  curl -v -X PROPFIND http://<tablet-ip>:<port>/ -H "Depth: 1"
  ```
- Stop logs with Ctrl-C.

## Troubleshooting

- No device in `adb devices`: re-pair wireless debugging.
- Download fails: the CI may not have produced an artifact yet. Build on-device with the companion script or place an APK in `$HOME`.
- Auth hangs: use `echo TOKEN | gh auth login --with-token` instead of the interactive flow.
