#!/bin/sh
set -eu
[ "$#" -eq 1 ] || { echo "usage: $0 provider-plugin.zip" >&2; exit 2; }
PACKAGE=$1
[ -f "$PACKAGE" ] || { echo "plugin package not found" >&2; exit 1; }
case "$PACKAGE" in *.zip) ;; *) echo "plugin package must end in .zip" >&2; exit 1;; esac
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERIFICATION=$("$HERE/../bin/wepush-agent" --verify-plugin "$PACKAGE")
NAME=$(printf '%s\n' "$VERIFICATION" | sed -n 's/.*"canonicalName":"\([A-Za-z0-9._-]*\.zip\)".*/\1/p')
[ -n "$NAME" ] || { echo "verified plugin did not return a canonical name" >&2; exit 1; }
if [ -n "${WEPUSH_PLUGIN_ROOT:-}" ]; then ROOT=$WEPUSH_PLUGIN_ROOT
elif [ "$(uname -s)" = Darwin ]; then ROOT=/Library/WePushNext/data/agent/plugins
else ROOT=/var/lib/wepush-next/agent/plugins
fi
install -d -m 0750 "$ROOT/staging"
if [ "$(id -u)" -eq 0 ]; then
  if [ "$(uname -s)" = Darwin ]; then PLUGIN_OWNER=$(stat -f '%u:%g' "$ROOT")
  else PLUGIN_OWNER=$(stat -c '%u:%g' "$ROOT"); fi
  chown "$PLUGIN_OWNER" "$ROOT/staging"
fi
TEMP="$ROOT/staging/.$NAME.tmp.$$"
install -m 0640 "$PACKAGE" "$TEMP"
if [ "$(id -u)" -eq 0 ]; then chown "$PLUGIN_OWNER" "$TEMP"; fi
mv "$TEMP" "$ROOT/staging/$NAME"
if command -v sha256sum >/dev/null 2>&1; then sha256sum "$ROOT/staging/$NAME"
else shasum -a 256 "$ROOT/staging/$NAME"
fi
echo "$VERIFICATION"
echo "Verified signature and staged $NAME"
