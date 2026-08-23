#!/bin/sh
set -eu
[ "$#" -eq 1 ] || { echo "usage: $0 staged-plugin.zip" >&2; exit 2; }
NAME=$(basename "$1")
if [ -n "${WEPUSH_PLUGIN_ROOT:-}" ]; then ROOT=$WEPUSH_PLUGIN_ROOT
elif [ "$(uname -s)" = Darwin ]; then ROOT=/Library/WePushNext/data/agent/plugins
else ROOT=/var/lib/wepush-next/agent/plugins
fi
[ -f "$ROOT/staging/$NAME" ] || { echo "staged plugin not found: $NAME" >&2; exit 1; }
install -d -m 0750 "$ROOT/active" "$ROOT/rollback"
PREVIOUS=
if [ -f "$ROOT/active/$NAME" ]; then
  PREVIOUS="$ROOT/rollback/$NAME.$(date -u +%Y%m%dT%H%M%SZ)"
  mv "$ROOT/active/$NAME" "$PREVIOUS"
fi
mv "$ROOT/staging/$NAME" "$ROOT/active/$NAME"
healthy=true
if command -v systemctl >/dev/null 2>&1; then
  systemctl restart wepush-next-agent.service || healthy=false
  CHECKS=0
  while [ "$healthy" = true ] && [ "$CHECKS" -lt 10 ]; do
    sleep 1
    systemctl is-active --quiet wepush-next-agent.service || healthy=false
    CHECKS=$((CHECKS + 1))
  done
elif [ "$(uname -s)" = Darwin ]; then
  launchctl kickstart -k system/com.fangxuele.wepush-next.agent || healthy=false
  sleep 3
  launchctl print system/com.fangxuele.wepush-next.agent 2>/dev/null | grep -q 'state = running' || healthy=false
fi
if [ "$healthy" != true ]; then
  mv "$ROOT/active/$NAME" "$ROOT/staging/$NAME.failed"
  if [ -n "$PREVIOUS" ]; then mv "$PREVIOUS" "$ROOT/active/$NAME"; fi
  if command -v systemctl >/dev/null 2>&1; then systemctl restart wepush-next-agent.service || true
  elif [ "$(uname -s)" = Darwin ]; then launchctl kickstart -k system/com.fangxuele.wepush-next.agent || true
  fi
  echo "Plugin activation failed; previous version was restored" >&2
  exit 1
fi
echo "Activated and verified $NAME"
