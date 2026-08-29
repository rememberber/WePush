#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run as root" >&2; exit 1; }
DESTINATION=${1:-${WEPUSH_BACKUP_ROOT:-/var/backups/wepush-next}}
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/etc/wepush-next}
DATA_ROOT=${WEPUSH_DATA_ROOT:-/var/lib/wepush-next}
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/opt/wepush-next}
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
  if systemctl is-active --quiet wepush-next-agent.service; then AGENT_ACTIVE=true; systemctl stop wepush-next-agent.service; fi
  if systemctl is-active --quiet wepush-next-service.service; then SERVICE_ACTIVE=true; systemctl stop wepush-next-service.service; fi
fi
restore_services() {
  if [ "$SERVICE_ACTIVE" = true ]; then systemctl start wepush-next-service.service; fi
  if [ "$AGENT_ACTIVE" = true ]; then systemctl start wepush-next-agent.service; fi
}
TEMP=$(mktemp -d "${TMPDIR:-/tmp}/wepush-next-backup.XXXXXX")
BACKUP_ID=${TEMP##*.}
cleanup() { rm -rf "$TEMP"; restore_services; }
trap cleanup EXIT HUP INT TERM
install -d -m 0700 "$TEMP/payload/config" "$TEMP/payload/data"
cp -a "$CONFIG_ROOT/." "$TEMP/payload/config/"
cp -a "$DATA_ROOT/." "$TEMP/payload/data/"
VERSION=unknown
if [ -L "$INSTALL_ROOT/current" ]; then VERSION=$(basename "$(readlink "$INSTALL_ROOT/current")"); fi
cat > "$TEMP/BACKUP-MANIFEST" <<EOF
format=wepush-next-backup-v1
platform=linux
productVersion=$VERSION
createdAt=$STAMP
contents=config,database,master-key,artifacts,agent-identity,journal,outbox,plugins
EOF
(cd "$TEMP" && find payload -type f -print | LC_ALL=C sort | while IFS= read -r FILE; do sha256sum "$FILE"; done > SHA256SUMS)
ARCHIVE="$DESTINATION/wepush-next-$STAMP-$BACKUP_ID.tar.gz"
tar -C "$TEMP" -czf "$ARCHIVE" BACKUP-MANIFEST SHA256SUMS payload
chmod 0600 "$ARCHIVE"
"$(dirname -- "$0")/restore.sh" --validate-only "$ARCHIVE"
echo "$ARCHIVE"
