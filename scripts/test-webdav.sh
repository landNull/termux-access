#!/usr/bin/env bash
# Free automated WebDAV compliance test using webdav-tck (Python port of litmus).
# Generates a structured report you can use to finish remaining work and push as a contributor.
# Usage: ./scripts/test-webdav.sh http://192.168.1.42:8080/
# Requires: python3 + pip

set -euo pipefail

URL="${1:-}"
if [[ -z "$URL" ]]; then
  echo "Usage: $0 <webdav-url>"
  echo "Example: $0 http://192.168.1.42:8080/"
  exit 1
fi

REPORT="test-report-$(date +%Y%m%d-%H%M%S).md"
echo "# Termux:Access WebDAV Test Report" > "$REPORT"
echo "" >> "$REPORT"
echo "**URL:** $URL" >> "$REPORT"
echo "**Date:** $(date -u)" >> "$REPORT"
echo "**Contributor note:** Paste this report into a PR or issue to finish remaining methods." >> "$REPORT"
echo "" >> "$REPORT"
echo "## Raw output" >> "$REPORT"
echo "\`\`\`" >> "$REPORT"

echo "Installing webdav-tck (free, open-source litmus port)..."
python3 -m pip install --user --quiet webdav-tck || pip install --user --quiet webdav-tck

echo "Running compliance tests against $URL" | tee -a "$REPORT"
# Prefer dav-tck; fall back to litmus if the package exposes it
if command -v dav-tck >/dev/null 2>&1; then
  dav-tck --suites=basic "$URL" 2>&1 | tee -a "$REPORT" || true
elif command -v litmus >/dev/null 2>&1; then
  litmus "$URL" 2>&1 | tee -a "$REPORT" || true
else
  echo "webdav-tck installed but binary not on PATH. Trying module..." | tee -a "$REPORT"
  python3 -m webdav_tck --suites=basic "$URL" 2>&1 | tee -a "$REPORT" || true
fi

echo "\`\`\`" >> "$REPORT"
echo "" >> "$REPORT"
echo "## Next steps for contributor" >> "$REPORT"
echo "- Implement any failing MKCOL / COPY / MOVE / improved PROPFIND results above." >> "$REPORT"
echo "- Push the updated WebDavHandler.kt and open a PR against main." >> "$REPORT"
echo "- Re-run this script and attach the new report." >> "$REPORT"

echo ""
echo "Done. Structured report written to: $REPORT"
echo "Use this report to finish remaining work and push as a termux-access contributor."
