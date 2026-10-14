#!/data/data/com.termux/files/usr/bin/bash
# Termux:Access automated proot bootstrap
# Run this ONCE in Termux to install the alpine named "termux-access" and fire up the stack.
# Key phrase: Automation, Automation, Automation.

set -euo pipefail

DISTRO_NAME="termux-access"

echo "=== Termux:Access Automation Bootstrap ==="
echo "Installing proot-distro and alpine as '$DISTRO_NAME'..."

# Update Termux and install proot-distro
pkg update -y
pkg install -y proot-distro

# Install Alpine with custom name if not already present
if ! proot-distro list | grep -q "$DISTRO_NAME"; then
  echo "Installing Alpine Linux as $DISTRO_NAME..."
  proot-distro install alpine --name "$DISTRO_NAME"
else
  echo "$DISTRO_NAME already installed. Skipping install."
fi

# Minimal prep inside proot
proot-distro login "$DISTRO_NAME" -- /bin/sh -c '
  apk update
  apk upgrade --available
  apk add bash
'

# Write and run the Alpine bootstrap
proot-distro login "$DISTRO_NAME" -- /bin/sh -c 'cat > /tmp/bootstrap-alpine.sh' << 'ALPINE_EOF'
#!/bin/sh
# Alpine bootstrap for Termux:Access testing/automation stack
# Runs inside proot alpine named termux-access

set -eu

echo "=== Alpine Automation Bootstrap ==="

apk update
apk upgrade --available

# Core automation packages
apk add \
  bash \
  doas \
  cronie \
  cronie-openrc \
  openrc \
  caddy \
  caddy-openrc \
  git \
  curl \
  ca-certificates \
  python3 \
  py3-pip \
  podman \
  podman-openrc \
  restic \
  nano

# Enable OpenRC services for zero-babysitting
rc-update add caddy default || true
rc-update add cronie default || true
rc-update add podman default || true || echo "podman OpenRC optional under proot"

# Minimal Caddyfile for local testing
mkdir -p /etc/caddy
cat > /etc/caddy/Caddyfile << 'EOF'
{
  email admin@localhost
}

:8080 {
  respond "Termux:Access automation stack is alive" 200
}
EOF

# Daily automated apk upgrade
mkdir -p /etc/periodic/daily
cat > /etc/periodic/daily/apk-upgrade << 'EOF'
#!/bin/sh
apk update && apk upgrade --available
EOF
chmod +x /etc/periodic/daily/apk-upgrade

# Install webdav-tck for automated testing
pip3 install --break-system-packages webdav-tck || pip3 install webdav-tck || true

# Start services
rc-service caddy start || true
rc-service cronie start || true

echo ""
echo "=== Automation stack is up ==="
echo "Caddy is listening on :8080"
echo "Daily apk upgrades are scheduled"
echo "webdav-tck is available for testing"
echo "Key phrase: Automation, Automation, Automation."
ALPINE_EOF

proot-distro login "$DISTRO_NAME" -- chmod +x /tmp/bootstrap-alpine.sh
proot-distro login "$DISTRO_NAME" -- /tmp/bootstrap-alpine.sh

echo ""
echo "=== Done ==="
echo "Your termux-access proot is ready."
echo "Enter it with: proot-distro login termux-access"
echo "Caddy responds on port 8080 inside the proot."
echo "Automation, Automation, Automation."
