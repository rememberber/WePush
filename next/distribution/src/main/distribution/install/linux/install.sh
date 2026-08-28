#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || [ "${WEPUSH_ALLOW_NON_ROOT:-false}" = true ] || { echo "run as root" >&2; exit 1; }
COMPONENT=${1:-standalone}
case "$COMPONENT" in standalone|service|agent|all) ;; *) echo "usage: $0 [standalone|service|agent|all]" >&2; exit 2;; esac
if [ "$COMPONENT" = standalone ]; then COMPONENT=service; fi
SOURCE=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
VERSION=$(basename "$SOURCE")
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/opt/wepush-next}
CONFIG_ROOT=${WEPUSH_CONFIG_ROOT:-/etc/wepush-next}
DATA_ROOT=${WEPUSH_DATA_ROOT:-/var/lib/wepush-next}
LOG_ROOT=${WEPUSH_LOG_ROOT:-/var/log/wepush-next}
RELEASE="$INSTALL_ROOT/releases/$VERSION"
if [ -x "$SOURCE/runtime/bin/java" ]; then JAVA="$SOURCE/runtime/bin/java"
else JAVA=${WEPUSH_JAVA:-java}; fi
command -v "$JAVA" >/dev/null 2>&1 || { echo "Java 21+ or a distribution with bundled runtime is required" >&2; exit 1; }
JAVA_MAJOR=$($JAVA -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
[ "${JAVA_MAJOR:-0}" -ge 21 ] || { echo "Java 21+ is required" >&2; exit 1; }
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  getent group wepush >/dev/null 2>&1 || groupadd --system wepush
  id wepush >/dev/null 2>&1 || useradd --system --gid wepush --home "$DATA_ROOT/service" --shell /usr/sbin/nologin wepush
  getent group wepush-agent >/dev/null 2>&1 || groupadd --system wepush-agent
  id wepush-agent >/dev/null 2>&1 || useradd --system --gid wepush-agent --home "$DATA_ROOT/agent" --shell /usr/sbin/nologin wepush-agent
fi
install -d -m 0755 "$INSTALL_ROOT/releases" "$CONFIG_ROOT" "$LOG_ROOT"
install -d -m 0750 "$DATA_ROOT/service" "$DATA_ROOT/service/tmp" "$DATA_ROOT/agent" "$DATA_ROOT/agent/tmp" "$DATA_ROOT/agent/plugins/active"
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  chown -R wepush:wepush "$DATA_ROOT/service"
  chown -R wepush-agent:wepush-agent "$DATA_ROOT/agent"
fi
if [ ! -d "$RELEASE" ]; then
  install -d -m 0755 "$RELEASE"
  for ITEM in bin lib install plugins config runtime; do
    if [ -e "$SOURCE/$ITEM" ]; then cp -R "$SOURCE/$ITEM" "$RELEASE/"; fi
  done
  if [ -d "$SOURCE/web" ]; then cp -R "$SOURCE/web" "$RELEASE/"; fi
  chmod 0755 "$RELEASE/bin/"* "$RELEASE/install/linux/"*.sh "$RELEASE/plugins/"*.sh
fi
ln -sfn "$RELEASE" "$INSTALL_ROOT/current.new"
mv -Tf "$INSTALL_ROOT/current.new" "$INSTALL_ROOT/current"
if [ ! -f "$CONFIG_ROOT/service.env" ]; then
  sed "s#/var/lib/wepush-next#$DATA_ROOT#g" "$SOURCE/config/service.env.example" > "$CONFIG_ROOT/service.env"
  chmod 0640 "$CONFIG_ROOT/service.env"
fi
if [ ! -f "$CONFIG_ROOT/agent.env" ]; then
  sed "s#/var/lib/wepush-next#$DATA_ROOT#g" "$SOURCE/config/agent.env.example" > "$CONFIG_ROOT/agent.env"
  chmod 0640 "$CONFIG_ROOT/agent.env"
fi
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" != true ]; then
  chown root:wepush "$CONFIG_ROOT/service.env"
  chown root:wepush-agent "$CONFIG_ROOT/agent.env"
  for UNIT in service agent; do
    if [ "$COMPONENT" = all ] || [ "$COMPONENT" = "$UNIT" ]; then
      install -m 0644 "$SOURCE/install/linux/wepush-next-$UNIT.service" "/etc/systemd/system/wepush-next-$UNIT.service"
    fi
  done
  systemctl daemon-reload
  for UNIT in service agent; do
    if [ "$COMPONENT" = all ] || [ "$COMPONENT" = "$UNIT" ]; then systemctl enable --now "wepush-next-$UNIT.service"; fi
  done
fi
echo "Installed WePush Next $VERSION ($COMPONENT)"
