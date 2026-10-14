# How to run WebDAV compliance tests

## Goal

Produce a structured test report you can use to finish remaining Class 1 methods and push as a contributor.

## Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/landNull/termux-access.git
   cd termux-access
   ```
2. Make the test script executable:
   ```bash
   chmod +x scripts/test-webdav.sh
   ```
3. Start the Termux:Access server and note its URL.
4. Run the test:
   ```bash
   ./scripts/test-webdav.sh http://<device-ip>:<port>/
   ```
5. Open the generated `test-report-*.md` file.
6. Implement any failing methods in `WebDavHandler.kt`.
7. Push your changes and re-run the test.

The report is designed so a contributor can take the results and finish the implementation.
