# Testing Termux:Access

## Install and run the free automated WebDAV test script

### Prerequisites
- Python 3 and pip on the machine that will run the tests (your computer or a CI runner).
- The Termux:Access server running on a device or emulator.
- Network reachability: same LAN, or `adb reverse` for an emulator.

### Install
```bash
# Clone the repo (if you do not already have it)
git clone https://github.com/landNull/termux-access.git
cd termux-access

# Make the script executable
chmod +x scripts/test-webdav.sh
```

The script itself installs **webdav-tck** (free Python port of litmus) the first time it runs. You can also install it manually:
```bash
python3 -m pip install --user webdav-tck
```

### Run
```bash
# Replace with the actual IP and port shown by the Termux:Access service
./scripts/test-webdav.sh http://192.168.1.42:8080/

# Emulator with port forwarding
adb reverse tcp:8080 tcp:8080
./scripts/test-webdav.sh http://127.0.0.1:8080/
```

The script runs the basic compliance suite (OPTIONS, PROPFIND, GET, PUT, DELETE) and prints pass/fail results.

### Classic litmus (optional)
```bash
git clone https://github.com/notroj/litmus
cd litmus && ./autogen.sh && ./configure && make
./litmus http://<device-ip>:<port>/
```

## GitHub Actions

`.github/workflows/build.yml` builds the debug APK on every push/PR and uploads it as an artifact. A protocol-test job is present (disabled by default) and can run webdav-tck when a `WEBDAV_TEST_URL` secret is set.

## Android notes

- Sign with the same key as Termux to test as a true add-on.
- Confirm the server only starts on explicit user action.
- Confirm path traversal is rejected.

MKCOL, COPY, and MOVE remain for full Class 1 coverage.
