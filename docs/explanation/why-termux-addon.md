# Explanation: Why a Termux add-on

Termux:Access is deliberately a Termux add-on (`sharedUserId="com.termux"`, same signing key) rather than a normal Android app.

## Why

- It needs direct access to Termux’s private filesystem (`$HOME`) without the FUSE/shared-storage problems that affect `/sdcard`.
- Shared UID lets it operate inside the same Linux user context as Termux, so Git repositories stay on the native ext4/f2fs filesystem.
- The DocumentsProvider and WebDAV server then expose a controlled subtree (`Apps`) to other apps and to the LAN.

## Trade-offs

- Must be signed with the same key as the main Termux app.
- Users must trust the add-on with the same privileges as Termux.
- This is the cleanest way to solve the original storage-reliability problem without asking users to root their devices.

See also the [reference](../reference/webdav-endpoints.md) for the exact HTTP contract.
