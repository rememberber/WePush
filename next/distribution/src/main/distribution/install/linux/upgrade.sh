#!/bin/sh
set -eu
[ "$#" -eq 2 ] || { echo "usage: $0 release.tar.gz expected-sha256" >&2; exit 2; }
ARCHIVE=$1
EXPECTED=$2
ACTUAL=$(sha256sum "$ARCHIVE" | awk '{print $1}')
[ "$ACTUAL" = "$EXPECTED" ] || { echo "release SHA-256 mismatch" >&2; exit 1; }
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
"$HERE/backup.sh"
TEMP=$(mktemp -d /tmp/wepush-next-upgrade.XXXXXX)
trap 'rm -rf "$TEMP"' EXIT HUP INT TERM
tar -xzf "$ARCHIVE" -C "$TEMP"
RELEASE=$(find "$TEMP" -mindepth 1 -maxdepth 1 -type d | head -1)
"$RELEASE/install/linux/install.sh" all
