#!/bin/sh
set -eu
[ "$#" -eq 1 ] || { echo "usage: $0 plugin-name.zip" >&2; exit 2; }
NAME=$(basename "$1")
if [ -n "${WEPUSH_PLUGIN_ROOT:-}" ]; then ROOT=$WEPUSH_PLUGIN_ROOT
elif [ "$(uname -s)" = Darwin ]; then ROOT=/Library/WePushNext/data/agent/plugins
else ROOT=/var/lib/wepush-next/agent/plugins
fi
[ -d "$ROOT/rollback" ] || { echo "plugin rollback directory not found" >&2; exit 1; }
PREVIOUS=$(find "$ROOT/rollback" -type f -name "$NAME.*" -print | LC_ALL=C sort | tail -1)
[ -n "$PREVIOUS" ] || { echo "no rollback version found for $NAME" >&2; exit 1; }
install -d -m 0750 "$ROOT/active" "$ROOT/rollback"
if [ "$(id -u)" -eq 0 ]; then
  if [ "$(uname -s)" = Darwin ]; then PLUGIN_OWNER=$(stat -f '%u:%g' "$ROOT")
  else PLUGIN_OWNER=$(stat -c '%u:%g' "$ROOT"); fi
  chown "$PLUGIN_OWNER" "$ROOT/active" "$ROOT/rollback"
fi
CURRENT=
if [ -f "$ROOT/active/$NAME" ]; then
  CURRENT="$ROOT/rollback/$NAME.$(date -u +%Y%m%dT%H%M%SZ).replaced"
  mv "$ROOT/active/$NAME" "$CURRENT"
fi
mv "$PREVIOUS" "$ROOT/active/$NAME"
healthy=true
if [ "${WEPUSH_SKIP_SERVICE_CONTROL:-false}" = true ]; then
  :
elif command -v systemctl >/dev/null 2>&1; then
  systemctl restart wepush-next-agent.service || healthy=false
  sleep 3
  systemctl is-active --quiet wepush-next-agent.service || healthy=false
elif [ "$(uname -s)" = Darwin ]; then
  launchctl kickstart -k system/com.fangxuele.wepush-next.agent || healthy=false
  sleep 3
  launchctl print system/com.fangxuele.wepush-next.agent 2>/dev/null | grep -q 'state = running' || healthy=false
fi
if [ "$healthy" != true ]; then
  mv "$ROOT/active/$NAME" "$PREVIOUS"
  if [ -n "$CURRENT" ]; then mv "$CURRENT" "$ROOT/active/$NAME"; fi
  echo "Plugin rollback failed; active version was recovered" >&2
  exit 1
fi
echo "Rolled back and verified $NAME"
