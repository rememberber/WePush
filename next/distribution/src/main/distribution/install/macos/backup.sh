#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run with sudo" >&2; exit 1; }
DESTINATION=${1:-${WEPUSH_BACKUP_ROOT:-/Library/WePushNext/backups}}
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/Library/Preferences/wepush-next}
DATA_ROOT=${WEPUSH_DATA_ROOT:-/Library/WePushNext/data}
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/Library/WePushNext}
[ -d "$CONFIG_ROOT" ] && [ -d "$DATA_ROOT" ] || { echo "WePush configuration or data directory is missing" >&2; exit 1; }
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
install -d -m 0700 "$DESTINATION"
DESTINATION_REAL=$(cd "$DESTINATION" && pwd -P)
CONFIG_ROOT_REAL=$(cd "$CONFIG_ROOT" && pwd -P)
DATA_ROOT_REAL=$(cd "$DATA_ROOT" && pwd -P)
case "$DESTINATION_REAL/" in
  "$CONFIG_ROOT_REAL/"*|"$DATA_ROOT_REAL/"*)
    echo "backup destination must not be inside the configuration or data directory" >&2
    exit 1
    ;;
esac
SERVICE_ACTIVE=false
AGENT_ACTIVE=false
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  if launchctl print system/com.fangxuele.wepush-next.agent >/dev/null 2>&1; then
    AGENT_ACTIVE=true; launchctl bootout system/com.fangxuele.wepush-next.agent
  fi
  if launchctl print system/com.fangxuele.wepush-next.service >/dev/null 2>&1; then
    SERVICE_ACTIVE=true; launchctl bootout system/com.fangxuele.wepush-next.service
  fi
fi
restore_services() {
  if [ "$SERVICE_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.service.plist; fi
  if [ "$AGENT_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.agent.plist; fi
}
TEMP=$(mktemp -d "${TMPDIR:-/tmp}/wepush-next-backup.XXXXXX")
cleanup() { rm -rf "$TEMP"; restore_services; }
trap cleanup EXIT HUP INT TERM
install -d -m 0700 "$TEMP/payload/config" "$TEMP/payload/data"
cp -a "$CONFIG_ROOT/." "$TEMP/payload/config/"
cp -a "$DATA_ROOT/." "$TEMP/payload/data/"
VERSION=unknown
if [ -L "$INSTALL_ROOT/current" ]; then VERSION=$(basename "$(readlink "$INSTALL_ROOT/current")"); fi
cat > "$TEMP/BACKUP-MANIFEST" <<EOF
format=wepush-next-backup-v1
platform=macos
productVersion=$VERSION
createdAt=$STAMP
contents=config,database,master-key,artifacts,agent-identity,journal,outbox,plugins
EOF
(cd "$TEMP" && find payload -type f -print | LC_ALL=C sort | while IFS= read -r FILE; do shasum -a 256 "$FILE"; done > SHA256SUMS)
ARCHIVE="$DESTINATION/wepush-next-$STAMP.tar.gz"
tar -C "$TEMP" -czf "$ARCHIVE" BACKUP-MANIFEST SHA256SUMS payload
chmod 0600 "$ARCHIVE"
"$(dirname -- "$0")/restore.sh" --validate-only "$ARCHIVE"
echo "$ARCHIVE"
