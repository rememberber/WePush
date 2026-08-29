#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run with sudo" >&2; exit 1; }
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/Library/WePushNext}
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/Library/Preferences/wepush-next}
DATA_ROOT=${WEPUSH_DATA_ROOT:-$INSTALL_ROOT/data}
LOG_ROOT=${WEPUSH_LOG_ROOT:-/Library/Logs/WePushNext}
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  for UNIT in service agent; do
    LABEL="com.fangxuele.wepush-next.$UNIT"
    launchctl bootout system/$LABEL 2>/dev/null || true
    rm -f "/Library/LaunchDaemons/$LABEL.plist"
  done
fi
if [ "${1:-}" = --purge ]; then
  echo "Purging WePush Next configuration and data"
  rm -rf "$INSTALL_ROOT" "$CONFIG_ROOT" "$DATA_ROOT" "$LOG_ROOT"
else
  rm -rf "$INSTALL_ROOT/releases" "$INSTALL_ROOT/current"
  echo "Services and binaries removed; configuration and data preserved. Pass --purge to remove them."
fi
