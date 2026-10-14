# Termux:Access

A Termux add-on that provides controlled access to the Termux private filesystem (`$HOME/Apps`) for other Android apps via the Storage Access Framework, and for trusted devices on the local network via a high-performance WebDAV server with mDNS discovery.

## Status

Core source implemented. DocumentsProvider, Ktor Netty service with streaming GET/PUT + Range, NsdManager, and basic UI are present. PROPFIND (basic) is implemented. Full Class 1 methods, optional auth, and complete grant-management UI remain as polish.

## Goals

- Keep Git repositories and development files on Termux’s native filesystem (ext4/f2fs) to avoid FUSE/shared-storage issues.
- Expose a controlled subtree (`$HOME/Apps`) to other Android apps via a DocumentsProvider.
- Make the same tree discoverable and accessible at high speed on a trusted LAN.
- Remain a proper Termux add-on (`sharedUserId="com.termux"`, same signing key).

## Naming

Repository and package follow official Termux add-on conventions:

- Repository: `termux-access`
- App label: **Termux:Access**
- Package: `com.termux.access`

## License

Released under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html) only, consistent with official Termux add-ons.

## Documentation (Diátaxis + DLitFunc)

Full documentation is organized with the [Diátaxis](https://diataxis.fr/) framework under the **DLitFunc** style (literal + functional synergistic programming and commenting).

See [docs/README.md](docs/README.md) for the map:

- [Tutorials](docs/tutorials/)
- [How-to guides](docs/how-to/)
- [Reference](docs/reference/)
- [Explanation](docs/explanation/)
- [DLitFunc style guide](docs/dlitfunc.md)

## Build

Open in Android Studio or build with Gradle. Must be signed with the same key as the main Termux app to function as a true add-on.

## Testing

See [TESTING.md](TESTING.md) and [docs/how-to/run-webdav-tests.md](docs/how-to/run-webdav-tests.md) for the free automated WebDAV compliance script.

## Security

- Network server starts only on explicit user action and is off by default.
- Intended for trusted local networks only.
- Path traversal is rejected on both SAF and WebDAV paths.
- Only the `Apps` subtree is served.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the DLitFunc organization steps.
