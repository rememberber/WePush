#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || { echo "run with sudo" >&2; exit 1; }
DESTINATION=${1:-/Library/WePushNext/backups}
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
install -d -m 0700 "$DESTINATION"
SERVICE_ACTIVE=false
AGENT_ACTIVE=false
if launchctl print system/com.fangxuele.wepush-next.agent >/dev/null 2>&1; then
  AGENT_ACTIVE=true
  launchctl bootout system/com.fangxuele.wepush-next.agent
fi
if launchctl print system/com.fangxuele.wepush-next.service >/dev/null 2>&1; then
  SERVICE_ACTIVE=true
  launchctl bootout system/com.fangxuele.wepush-next.service
fi
restore_services() {
  if [ "$SERVICE_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.service.plist; fi
  if [ "$AGENT_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.agent.plist; fi
}
trap restore_services EXIT
trap 'exit 1' HUP INT TERM
tar -C / -czf "$DESTINATION/wepush-next-$STAMP.tar.gz" Library/Preferences/wepush-next Library/WePushNext/data
chmod 0600 "$DESTINATION/wepush-next-$STAMP.tar.gz"
echo "$DESTINATION/wepush-next-$STAMP.tar.gz"
