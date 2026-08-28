#!/bin/sh
set -eu
VALIDATE_ONLY=false
if [ "${1:-}" = --validate-only ]; then VALIDATE_ONLY=true; shift; fi
[ "$#" -ge 1 ] && [ "$#" -le 2 ] || { echo "usage: $0 [--validate-only] backup.tar.gz [expected-sha256]" >&2; exit 2; }
ARCHIVE=$1
[ -f "$ARCHIVE" ] || { echo "backup archive not found" >&2; exit 1; }
if [ "$#" -eq 2 ]; then
  ACTUAL=$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')
  [ "$ACTUAL" = "$2" ] || { echo "backup SHA-256 mismatch" >&2; exit 1; }
fi
tar -tzf "$ARCHIVE" | while IFS= read -r ENTRY; do
  case "$ENTRY" in /*|../*|*/../*|*/..) echo "unsafe backup entry: $ENTRY" >&2; exit 1;; esac
done
TEMP=$(mktemp -d "${TMPDIR:-/tmp}/wepush-next-restore.XXXXXX")
cleanup() { rm -rf "$TEMP"; }
trap cleanup EXIT HUP INT TERM
tar -xzf "$ARCHIVE" -C "$TEMP"
[ "$(sed -n 's/^format=//p' "$TEMP/BACKUP-MANIFEST")" = wepush-next-backup-v1 ] || { echo "unsupported backup format" >&2; exit 1; }
[ "$(sed -n 's/^platform=//p' "$TEMP/BACKUP-MANIFEST")" = macos ] || { echo "backup platform mismatch" >&2; exit 1; }
MANIFEST_FILES="$TEMP/manifest-files"
ACTUAL_FILES="$TEMP/actual-files"
: > "$MANIFEST_FILES"
while IFS= read -r LINE || [ -n "$LINE" ]; do
  DIGEST=$(printf '%s\n' "$LINE" | cut -c1-64)
  SEPARATOR=$(printf '%s\n' "$LINE" | cut -c65-66)
  FILE=$(printf '%s\n' "$LINE" | cut -c67-)
  printf '%s\n' "$DIGEST" | grep -Eq '^[0-9a-f]{64}$' || { echo "invalid backup checksum digest" >&2; exit 1; }
  [ "$SEPARATOR" = "  " ] || { echo "invalid backup checksum separator" >&2; exit 1; }
  case "$FILE" in payload/config/*|payload/data/*) ;; *) echo "unsafe backup checksum path: $FILE" >&2; exit 1;; esac
  case "$FILE" in /*|../*|*/../*|*/..|*\\*) echo "unsafe backup checksum path: $FILE" >&2; exit 1;; esac
  printf '%s\n' "$FILE" >> "$MANIFEST_FILES"
done < "$TEMP/SHA256SUMS"
LC_ALL=C sort -o "$MANIFEST_FILES" "$MANIFEST_FILES"
[ -z "$(uniq -d "$MANIFEST_FILES")" ] || { echo "duplicate backup checksum path" >&2; exit 1; }
if find "$TEMP/payload" ! -type d ! -type f -print | grep -q .; then echo "backup payload contains a non-regular entry" >&2; exit 1; fi
(cd "$TEMP" && find payload -type f -print | LC_ALL=C sort) > "$ACTUAL_FILES"
cmp -s "$MANIFEST_FILES" "$ACTUAL_FILES" || { echo "backup payload file set differs from SHA256SUMS" >&2; exit 1; }
(cd "$TEMP" && shasum -a 256 -c SHA256SUMS >/dev/null)
[ -d "$TEMP/payload/config" ] && [ -d "$TEMP/payload/data" ] || { echo "backup payload is incomplete" >&2; exit 1; }
if [ "$VALIDATE_ONLY" = true ]; then echo "Backup validated: manifest, paths and all file checksums are valid"; exit 0; fi
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run with sudo" >&2; exit 1; }
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/Library/Preferences/wepush-next}
DATA_ROOT=${WEPUSH_DATA_ROOT:-/Library/WePushNext/data}
BACKUP_ROOT=${WEPUSH_BACKUP_ROOT:-/Library/WePushNext/backups}
case "$CONFIG_ROOT" in /*) ;; *) echo "configuration root must be absolute" >&2; exit 1;; esac
case "$DATA_ROOT" in /*) ;; *) echo "data root must be absolute" >&2; exit 1;; esac
[ "$CONFIG_ROOT" != / ] && [ "$DATA_ROOT" != / ] && [ "$CONFIG_ROOT" != "$DATA_ROOT" ] \
  || { echo "refusing unsafe restore roots" >&2; exit 1; }
install -d -m 0700 "$BACKUP_ROOT"
RECOVERY="$BACKUP_ROOT/pre-restore-$(date -u +%Y%m%dT%H%M%SZ)-$$"
case "$RECOVERY/" in "$CONFIG_ROOT/"*|"$DATA_ROOT/"*) echo "backup root must not be inside the restored paths" >&2; exit 1;; esac
install -d -m 0700 "$RECOVERY"
SERVICE_ACTIVE=false
AGENT_ACTIVE=false
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  if launchctl print system/com.fangxuele.wepush-next.agent >/dev/null 2>&1; then AGENT_ACTIVE=true; launchctl bootout system/com.fangxuele.wepush-next.agent; fi
  if launchctl print system/com.fangxuele.wepush-next.service >/dev/null 2>&1; then SERVICE_ACTIVE=true; launchctl bootout system/com.fangxuele.wepush-next.service; fi
fi
restore_old() {
  rm -rf "$CONFIG_ROOT" "$DATA_ROOT"
  if [ -e "$RECOVERY/config" ]; then mv "$RECOVERY/config" "$CONFIG_ROOT"; fi
  if [ -e "$RECOVERY/data" ]; then mv "$RECOVERY/data" "$DATA_ROOT"; fi
  if [ "$SERVICE_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.service.plist || true; fi
  if [ "$AGENT_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.agent.plist || true; fi
}
CONFIG_STAGE="${CONFIG_ROOT}.restore.$$"; DATA_STAGE="${DATA_ROOT}.restore.$$"
rm -rf "$CONFIG_STAGE" "$DATA_STAGE"
cp -a "$TEMP/payload/config" "$CONFIG_STAGE"; cp -a "$TEMP/payload/data" "$DATA_STAGE"
if [ -e "$CONFIG_ROOT" ]; then mv "$CONFIG_ROOT" "$RECOVERY/config"; fi
if [ -e "$DATA_ROOT" ]; then mv "$DATA_ROOT" "$RECOVERY/data"; fi
if ! mv "$CONFIG_STAGE" "$CONFIG_ROOT" || ! mv "$DATA_STAGE" "$DATA_ROOT"; then restore_old; echo "restore replacement failed; original data was recovered" >&2; exit 1; fi
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  if [ "$SERVICE_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.service.plist; fi
  if [ "$AGENT_ACTIVE" = true ]; then launchctl bootstrap system /Library/LaunchDaemons/com.fangxuele.wepush-next.agent.plist; fi
fi
if ! "$(dirname -- "$0")/../verify-install.sh" "${WEPUSH_EXPECTED_VERSION:-}"; then
  if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
    launchctl bootout system/com.fangxuele.wepush-next.agent 2>/dev/null || true
    launchctl bootout system/com.fangxuele.wepush-next.service 2>/dev/null || true
  fi
  restore_old
  echo "restored data failed health verification; original data was recovered" >&2
  exit 1
fi
echo "Backup restored and verified; pre-restore data retained at $RECOVERY"
