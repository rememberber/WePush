#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || { echo "run with sudo" >&2; exit 1; }
for UNIT in service agent; do
  LABEL="com.fangxuele.wepush-next.$UNIT"
  launchctl bootout system/$LABEL 2>/dev/null || true
  rm -f "/Library/LaunchDaemons/$LABEL.plist"
done
if [ "${1:-}" = --purge ]; then
  echo "Purging WePush Next configuration and data"
  rm -rf /Library/WePushNext /Library/Preferences/wepush-next /Library/Logs/WePushNext
else
  rm -rf /Library/WePushNext/releases /Library/WePushNext/current
  echo "Services and binaries removed; configuration and data preserved. Pass --purge to remove them."
fi
