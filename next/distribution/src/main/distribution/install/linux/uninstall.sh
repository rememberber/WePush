#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run as root" >&2; exit 1; }
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/opt/wepush-next}
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/etc/wepush-next}
DATA_ROOT=${WEPUSH_DATA_ROOT:-/var/lib/wepush-next}
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  for UNIT in service agent; do systemctl disable --now "wepush-next-$UNIT.service" 2>/dev/null || true; done
  rm -f /etc/systemd/system/wepush-next-service.service /etc/systemd/system/wepush-next-agent.service
  systemctl daemon-reload
fi
if [ "${1:-}" = --purge ]; then
  echo "Purging $CONFIG_ROOT and $DATA_ROOT"
  rm -rf "$CONFIG_ROOT" "$DATA_ROOT" "$INSTALL_ROOT"
else
  echo "Binaries and services removed; configuration and data preserved. Pass --purge to remove them."
  rm -rf "$INSTALL_ROOT"
fi
