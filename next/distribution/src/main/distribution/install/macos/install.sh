#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || { echo "run with sudo" >&2; exit 1; }
COMPONENT=${1:-all}
case "$COMPONENT" in service|agent|all) ;; *) echo "usage: $0 [service|agent|all]" >&2; exit 2;; esac
OWNER=${WEPUSH_SERVICE_USER:-${SUDO_USER:-}}
[ -n "$OWNER" ] && [ "$OWNER" != root ] || {
  echo "a non-root service user is required; run with sudo or set WEPUSH_SERVICE_USER" >&2
  exit 1
}
id "$OWNER" >/dev/null 2>&1 || { echo "service user does not exist: $OWNER" >&2; exit 1; }
SOURCE=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
VERSION=$(basename "$SOURCE")
RELEASE="/Library/WePushNext/releases/$VERSION"
command -v java >/dev/null 2>&1 || { echo "Java 21+ is required" >&2; exit 1; }
JAVA_MAJOR=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
[ "${JAVA_MAJOR:-0}" -ge 21 ] || { echo "Java 21+ is required" >&2; exit 1; }
install -d -m 0755 /Library/WePushNext/releases /Library/Preferences/wepush-next /Library/Logs/WePushNext
install -d -o "$OWNER" -m 0750 /Library/WePushNext/data/service /Library/WePushNext/data/agent /Library/WePushNext/data/service/tmp /Library/WePushNext/data/agent/tmp /Library/WePushNext/data/agent/plugins/active
if [ ! -d "$RELEASE" ]; then
  install -d -m 0755 "$RELEASE"
  cp -R "$SOURCE/bin" "$SOURCE/lib" "$SOURCE/install" "$SOURCE/plugins" "$SOURCE/config" "$RELEASE/"
  if [ -d "$SOURCE/web" ]; then cp -R "$SOURCE/web" "$RELEASE/"; fi
  chmod 0755 "$RELEASE/bin/"* "$RELEASE/install/macos/"*.sh "$RELEASE/plugins/"*.sh
fi
ln -sfn "$RELEASE" /Library/WePushNext/current.new
if [ -L /Library/WePushNext/current ]; then unlink /Library/WePushNext/current
elif [ -e /Library/WePushNext/current ]; then echo "refusing to replace non-link current path" >&2; exit 1
fi
mv /Library/WePushNext/current.new /Library/WePushNext/current
if [ ! -f /Library/Preferences/wepush-next/service.env ]; then
  sed 's#/var/lib/wepush-next/service#/Library/WePushNext/data/service#g' "$SOURCE/config/service.env.example" > /Library/Preferences/wepush-next/service.env
  chown "$OWNER":wheel /Library/Preferences/wepush-next/service.env
  chmod 0600 /Library/Preferences/wepush-next/service.env
fi
if [ ! -f /Library/Preferences/wepush-next/agent.env ]; then
  sed 's#/var/lib/wepush-next/agent#/Library/WePushNext/data/agent#g' "$SOURCE/config/agent.env.example" > /Library/Preferences/wepush-next/agent.env
  chown "$OWNER":wheel /Library/Preferences/wepush-next/agent.env
  chmod 0600 /Library/Preferences/wepush-next/agent.env
fi
for UNIT in service agent; do
  if [ "$COMPONENT" = all ] || [ "$COMPONENT" = "$UNIT" ]; then
    LABEL="com.fangxuele.wepush-next.$UNIT"
    PLIST="/Library/LaunchDaemons/$LABEL.plist"
    launchctl bootout system/$LABEL 2>/dev/null || true
    sed "s/__WEPUSH_USER__/$OWNER/g" "$SOURCE/install/macos/$LABEL.plist" > "$PLIST"
    chmod 0644 "$PLIST"; chown root:wheel "$PLIST"
    plutil -lint "$PLIST"
    launchctl bootstrap system "$PLIST"
  fi
done
echo "Installed WePush Next $VERSION ($COMPONENT)"
