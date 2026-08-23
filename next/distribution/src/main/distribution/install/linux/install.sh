#!/bin/sh
set -eu
[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }
COMPONENT=${1:-all}
case "$COMPONENT" in service|agent|all) ;; *) echo "usage: $0 [service|agent|all]" >&2; exit 2;; esac
SOURCE=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
VERSION=$(basename "$SOURCE")
RELEASE="/opt/wepush-next/releases/$VERSION"
command -v java >/dev/null 2>&1 || { echo "Java 21+ is required" >&2; exit 1; }
JAVA_MAJOR=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
[ "${JAVA_MAJOR:-0}" -ge 21 ] || { echo "Java 21+ is required" >&2; exit 1; }
getent group wepush >/dev/null 2>&1 || groupadd --system wepush
id wepush >/dev/null 2>&1 || useradd --system --gid wepush --home /var/lib/wepush-next/service --shell /usr/sbin/nologin wepush
getent group wepush-agent >/dev/null 2>&1 || groupadd --system wepush-agent
id wepush-agent >/dev/null 2>&1 || useradd --system --gid wepush-agent --home /var/lib/wepush-next/agent --shell /usr/sbin/nologin wepush-agent
install -d -m 0755 /opt/wepush-next/releases /etc/wepush-next /var/log/wepush-next
install -d -o wepush -g wepush -m 0750 /var/lib/wepush-next/service /var/lib/wepush-next/service/tmp
install -d -o wepush-agent -g wepush-agent -m 0750 /var/lib/wepush-next/agent /var/lib/wepush-next/agent/tmp /var/lib/wepush-next/agent/plugins/active
if [ ! -d "$RELEASE" ]; then
  install -d -m 0755 "$RELEASE"
  cp -R "$SOURCE/bin" "$SOURCE/lib" "$SOURCE/install" "$SOURCE/plugins" "$SOURCE/config" "$RELEASE/"
  if [ -d "$SOURCE/web" ]; then cp -R "$SOURCE/web" "$RELEASE/"; fi
  chmod 0755 "$RELEASE/bin/"* "$RELEASE/install/linux/"*.sh "$RELEASE/plugins/"*.sh
fi
ln -sfn "$RELEASE" /opt/wepush-next/current.new
mv -Tf /opt/wepush-next/current.new /opt/wepush-next/current
if [ ! -f /etc/wepush-next/service.env ]; then
  install -o root -g wepush -m 0640 "$SOURCE/config/service.env.example" /etc/wepush-next/service.env
fi
if [ ! -f /etc/wepush-next/agent.env ]; then
  install -o root -g wepush-agent -m 0640 "$SOURCE/config/agent.env.example" /etc/wepush-next/agent.env
fi
for UNIT in service agent; do
  if [ "$COMPONENT" = all ] || [ "$COMPONENT" = "$UNIT" ]; then
    install -m 0644 "$SOURCE/install/linux/wepush-next-$UNIT.service" "/etc/systemd/system/wepush-next-$UNIT.service"
  fi
done
systemctl daemon-reload
for UNIT in service agent; do
  if [ "$COMPONENT" = all ] || [ "$COMPONENT" = "$UNIT" ]; then systemctl enable --now "wepush-next-$UNIT.service"; fi
done
echo "Installed WePush Next $VERSION ($COMPONENT)"
