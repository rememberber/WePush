#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
for UNIT in service agent; do systemctl disable --now "wepush-next-$UNIT.service" 2>/dev/null || true; done
rm -f /etc/systemd/system/wepush-next-service.service /etc/systemd/system/wepush-next-agent.service
systemctl daemon-reload
if [ "${1:-}" = --purge ]; then
  echo "Purging /etc/wepush-next and /var/lib/wepush-next"
  rm -rf /etc/wepush-next /var/lib/wepush-next /opt/wepush-next
else
  echo "Binaries and services removed; configuration and data preserved. Pass --purge to remove them."
  rm -rf /opt/wepush-next
fi
