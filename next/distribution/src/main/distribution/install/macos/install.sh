#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run with sudo" >&2; exit 1; }
COMPONENT=${1:-standalone}
case "$COMPONENT" in standalone|service|agent|all) ;; *) echo "usage: $0 [standalone|service|agent|all]" >&2; exit 2;; esac
if [ "$COMPONENT" = standalone ]; then COMPONENT=service; fi
OWNER=${WEPUSH_SERVICE_USER:-${SUDO_USER:-}}
[ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" = true ] || { [ -n "$OWNER" ] && [ "$OWNER" != root ]; } || {
  echo "a non-root service user is required; run with sudo or set WEPUSH_SERVICE_USER" >&2
  exit 1
}
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  id "$OWNER" >/dev/null 2>&1 || { echo "service user does not exist: $OWNER" >&2; exit 1; }
fi
SOURCE=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
VERSION=$(basename "$SOURCE")
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/Library/WePushNext}
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/Library/Preferences/wepush-next}
LOG_ROOT=${WEPUSH_LOG_ROOT:-/Library/Logs/WePushNext}
DATA_ROOT=${WEPUSH_DATA_ROOT:-$INSTALL_ROOT/data}
RELEASE="$INSTALL_ROOT/releases/$VERSION"
if [ -x "$SOURCE/runtime/bin/java" ]; then JAVA="$SOURCE/runtime/bin/java"
else JAVA=${WEPUSH_JAVA:-java}; fi
command -v "$JAVA" >/dev/null 2>&1 || { echo "Java 21+ or a distribution with bundled runtime is required" >&2; exit 1; }
JAVA_MAJOR=$("$JAVA" -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
[ "${JAVA_MAJOR:-0}" -ge 21 ] || { echo "Java 21+ is required" >&2; exit 1; }
install -d -m 0755 "$INSTALL_ROOT/releases" "$CONFIG_ROOT" "$LOG_ROOT"
install -d -m 0750 "$DATA_ROOT/service" "$DATA_ROOT/agent" "$DATA_ROOT/service/tmp" "$DATA_ROOT/agent/tmp" "$DATA_ROOT/agent/plugins/active"
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then chown -R "$OWNER" "$DATA_ROOT"; fi
if [ ! -d "$RELEASE" ]; then
  install -d -m 0755 "$RELEASE"
  for ITEM in bin lib install plugins config runtime; do
    if [ -e "$SOURCE/$ITEM" ]; then cp -R "$SOURCE/$ITEM" "$RELEASE/"; fi
  done
  if [ -d "$SOURCE/web" ]; then cp -R "$SOURCE/web" "$RELEASE/"; fi
  chmod 0755 "$RELEASE/bin/"* "$RELEASE/install/macos/"*.sh "$RELEASE/plugins/"*.sh
fi
ln -sfn "$RELEASE" "$INSTALL_ROOT/current.new"
if [ -L "$INSTALL_ROOT/current" ]; then unlink "$INSTALL_ROOT/current"
elif [ -e "$INSTALL_ROOT/current" ]; then echo "refusing to replace non-link current path" >&2; exit 1
fi
mv "$INSTALL_ROOT/current.new" "$INSTALL_ROOT/current"
if [ ! -f "$CONFIG_ROOT/service.env" ]; then
  sed "s#/var/lib/wepush-next#$DATA_ROOT#g" "$SOURCE/config/service.env.example" > "$CONFIG_ROOT/service.env"
  chmod 0600 "$CONFIG_ROOT/service.env"
fi
if [ ! -f "$CONFIG_ROOT/agent.env" ]; then
  sed "s#/var/lib/wepush-next#$DATA_ROOT#g" "$SOURCE/config/agent.env.example" > "$CONFIG_ROOT/agent.env"
  chmod 0600 "$CONFIG_ROOT/agent.env"
fi
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  chown "$OWNER":wheel "$CONFIG_ROOT/service.env" "$CONFIG_ROOT/agent.env"
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
fi
echo "Installed WePush Next $VERSION ($COMPONENT)"
