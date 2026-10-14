#!/bin/sh
# Install tools that automate DLitFunc (Diátaxis + literal + functional style)
# Run inside the termux-access Alpine proot or on a developer machine.

set -eu

echo "=== Installing DLitFunc automation tools ==="

# Vale for Diátaxis documentation linting
if ! command -v vale >/dev/null 2>&1; then
  echo "Installing Vale..."
  # Alpine / static binary approach
  curl -fsSL https://github.com/errata-ai/vale/releases/download/v3.7.0/vale_3.7.0_Linux_64-bit.tar.gz -o /tmp/vale.tar.gz || true
  if [ -f /tmp/vale.tar.gz ]; then
    tar -xzf /tmp/vale.tar.gz -C /usr/local/bin vale
    rm /tmp/vale.tar.gz
  else
    echo "Download Vale manually from https://github.com/errata-ai/vale/releases"
  fi
fi

# Sync Diátaxis styles
if command -v vale >/dev/null 2>&1; then
  vale sync || true
fi

# ktlint (Kotlin formatter/linter)
if ! command -v ktlint >/dev/null 2>&1; then
  echo "Installing ktlint..."
  curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.3.1/ktlint
  chmod +x ktlint
  mv ktlint /usr/local/bin/ || mv ktlint ~/bin/ || true
fi

echo ""
echo "Tools ready."
echo "Run 'vale docs' to lint Diátaxis documentation."
echo "Run 'ktlint --format' on Kotlin sources for literal/functional style."
echo "See docs/dlitfunc.md for the full approach."
