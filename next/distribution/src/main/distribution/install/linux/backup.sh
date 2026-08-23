#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
DESTINATION=${1:-/var/backups/wepush-next}
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
install -d -m 0700 "$DESTINATION"
SERVICE_ACTIVE=false
AGENT_ACTIVE=false
if systemctl is-active --quiet wepush-next-agent.service; then AGENT_ACTIVE=true; systemctl stop wepush-next-agent.service; fi
if systemctl is-active --quiet wepush-next-service.service; then SERVICE_ACTIVE=true; systemctl stop wepush-next-service.service; fi
restore_services() {
  if [ "$SERVICE_ACTIVE" = true ]; then systemctl start wepush-next-service.service; fi
  if [ "$AGENT_ACTIVE" = true ]; then systemctl start wepush-next-agent.service; fi
}
trap restore_services EXIT
trap 'exit 1' HUP INT TERM
tar -C / -czf "$DESTINATION/wepush-next-$STAMP.tar.gz" etc/wepush-next var/lib/wepush-next
chmod 0600 "$DESTINATION/wepush-next-$STAMP.tar.gz"
echo "$DESTINATION/wepush-next-$STAMP.tar.gz"
