# Testing Termux:Access

## WebDAV compliance

Once the server is running on a device (or emulator with port forwarding):

```bash
# Classic litmus suite
git clone https://github.com/notroj/litmus
cd litmus && ./autogen.sh && ./configure && make
./litmus http://<device-ip>:<port>/

# Modern Python port
pip install webdav-tck
# or clone https://github.com/abilian/webdav-tck
```

Current implementation supports OPTIONS, GET (with Range), PUT, DELETE, and path-traversal protection. PROPFIND is the next method needed for full litmus basic suite.

## Android

- Build the debug APK via the GitHub Actions workflow (`.github/workflows/build.yml`).
- Sign with the same key as Termux to test as a true add-on.
- Use Android Studio or `adb` to install and exercise the SAF DocumentsProvider and the foreground service.

## Security checks

- Confirm the server only starts on explicit user action.
- Confirm path traversal attempts (e.g. `../`) are rejected on both SAF and WebDAV paths.
