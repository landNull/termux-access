#!/usr/bin/env bash
# Free automated WebDAV compliance test using webdav-tck (Python port of litmus).
# Usage: ./scripts/test-webdav.sh http://192.168.1.42:8080/
# Requires: python3 + pip

set -euo pipefail

URL="${1:-}"
if [[ -z "$URL" ]]; then
  echo "Usage: $0 <webdav-url>"
  echo "Example: $0 http://192.168.1.42:8080/"
  exit 1
fi

echo "Installing webdav-tck (free, open-source litmus port)..."
python3 -m pip install --user --quiet webdav-tck || pip install --user --quiet webdav-tck

echo "Running compliance tests against $URL"
# Prefer dav-tck; fall back to litmus if the package exposes it
if command -v dav-tck >/dev/null 2>&1; then
  dav-tck --suites=basic "$URL" || true
elif command -v litmus >/dev/null 2>&1; then
  litmus "$URL" || true
else
  echo "webdav-tck installed but binary not on PATH. Try:"
  echo "  python3 -m webdav_tck $URL"
  python3 -m webdav_tck --suites=basic "$URL" || true
fi

echo "Done. See output above for pass/fail."
