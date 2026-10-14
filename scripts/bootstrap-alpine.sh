#!/bin/sh
# Alpine bootstrap for Termux:Access testing/automation stack
# Runs inside proot alpine named termux-access
# Key phrase: Automation, Automation, Automation.

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
