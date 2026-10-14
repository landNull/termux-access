# Tutorial: Getting started with Termux:Access

This lesson walks you through installing the add-on and starting the WebDAV server for the first time.

## Prerequisites

- Android device with Termux installed
- The Termux:Access APK signed with the same key as Termux (true add-on mode)

## Steps

1. Install the Termux:Access APK.
2. Open Termux:Access.
3. Tap the server toggle to start the WebDAV service.
4. Note the IP and port shown in the UI.
5. From another device on the same network, open `http://<ip>:<port>/` in a WebDAV client.

You now have a running, discoverable WebDAV endpoint serving `$HOME/Apps`.

## What you learned

- How to start the server
- How to reach it from the LAN

Next: [How to run WebDAV compliance tests](../how-to/run-webdav-tests.md)
