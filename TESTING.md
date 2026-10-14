# Testing Termux:Access

## Free automated WebDAV compliance (steps 1–3)

PROPFIND, OPTIONS, GET (Range), PUT, and DELETE are implemented. Use the free automated script:

```bash
# On the host machine (device and host on same network, or use adb reverse)
chmod +x scripts/test-webdav.sh
./scripts/test-webdav.sh http://<device-ip>:<port>/

# With emulator port forwarding:
# adb reverse tcp:8080 tcp:8080
# ./scripts/test-webdav.sh http://127.0.0.1:8080/
```

The script installs **webdav-tck** (free Python port of litmus) and runs the basic suite.

Classic litmus is also free:
```bash
git clone https://github.com/notroj/litmus
cd litmus && ./autogen.sh && ./configure && make
./litmus http://<device-ip>:<port>/
```

## GitHub Actions (free automated build)

`.github/workflows/build.yml` builds the debug APK on every push/PR and uploads it as an artifact. A protocol-test job is included (disabled by default) that can run webdav-tck when a `WEBDAV_TEST_URL` secret is set.

## Android

- Sign with the same key as Termux to test as a true add-on.
- Confirm the server only starts on explicit user action.
- Confirm path traversal is rejected.

MKCOL, COPY, and MOVE remain for full Class 1 coverage.
