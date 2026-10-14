# Termux:Access

A Termux add-on that provides controlled access to the Termux private filesystem (`$HOME/Apps`) for other Android apps via the Storage Access Framework, and for trusted devices on the local network via a high-performance WebDAV server with mDNS discovery.

## Status

Specification complete. Implementation pending.

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

## Specification

The full technical specification is maintained in the conversation history and local artifacts. A copy will be added to this repository as implementation begins.

## Important Notes

- Must be signed with the same key as the main Termux app to function as a true add-on.
- Network server is off by default and intended for trusted local networks only.
