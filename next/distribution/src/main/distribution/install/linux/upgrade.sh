#!/bin/sh
set -eu
[ "$#" -eq 2 ] || { echo "usage: $0 release.tar.gz expected-sha256" >&2; exit 2; }
ARCHIVE=$1
EXPECTED=$2
ACTUAL=$(sha256sum "$ARCHIVE" | awk '{print $1}')
[ "$ACTUAL" = "$EXPECTED" ] || { echo "release SHA-256 mismatch" >&2; exit 1; }
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INSTALL_ROOT=${WEPUSH_INSTALL_ROOT:-/opt/wepush-next}
PREVIOUS=
if [ -L "$INSTALL_ROOT/current" ]; then PREVIOUS=$(readlink "$INSTALL_ROOT/current"); fi
BACKUP=$("$HERE/backup.sh" | tail -1)
TEMP=$(mktemp -d "${TMPDIR:-/tmp}/wepush-next-upgrade.XXXXXX")
cleanup() { rm -rf "$TEMP"; }
trap cleanup EXIT HUP INT TERM
tar -xzf "$ARCHIVE" -C "$TEMP"
RELEASE=$(find "$TEMP" -mindepth 1 -maxdepth 1 -type d | head -1)
VERSION=$(basename "$RELEASE" | sed 's/^wepush-next-//')
if "$RELEASE/install/linux/install.sh" standalone \
    && "$RELEASE/install/verify-install.sh" "$VERSION"; then
  echo "Upgrade to $VERSION verified; backup retained at $BACKUP"
  exit 0
fi
echo "Upgrade verification failed; restoring previous release and data" >&2
if [ -n "$PREVIOUS" ]; then
  ln -sfn "$PREVIOUS" "$INSTALL_ROOT/current.new"
  mv -Tf "$INSTALL_ROOT/current.new" "$INSTALL_ROOT/current"
fi
WEPUSH_EXPECTED_VERSION= "$HERE/restore.sh" "$BACKUP"
exit 1
