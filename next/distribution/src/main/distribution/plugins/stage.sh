#!/bin/sh
set -eu
[ "$#" -eq 1 ] || { echo "usage: $0 provider-plugin.zip" >&2; exit 2; }
PACKAGE=$1
[ -f "$PACKAGE" ] || { echo "plugin package not found" >&2; exit 1; }
if [ -n "${WEPUSH_PLUGIN_ROOT:-}" ]; then ROOT=$WEPUSH_PLUGIN_ROOT
elif [ "$(uname -s)" = Darwin ]; then ROOT=/Library/WePushNext/data/agent/plugins
else ROOT=/var/lib/wepush-next/agent/plugins
fi
install -d -m 0750 "$ROOT/staging"
NAME=$(basename "$PACKAGE")
case "$NAME" in *.zip) ;; *) echo "plugin package must end in .zip" >&2; exit 1;; esac
TEMP="$ROOT/staging/.$NAME.tmp.$$"
install -m 0640 "$PACKAGE" "$TEMP"
mv "$TEMP" "$ROOT/staging/$NAME"
if command -v sha256sum >/dev/null 2>&1; then sha256sum "$ROOT/staging/$NAME"
else shasum -a 256 "$ROOT/staging/$NAME"
fi
echo "Staged $NAME. Signature and content are verified by Agent before loading."
