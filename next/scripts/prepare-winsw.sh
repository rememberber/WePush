#!/bin/sh
set -eu

VERSION=2.12.0
EXPECTED_LENGTH=18243033
EXPECTED_SHA256=05b82d46ad331cc16bdc00de5c6332c1ef818df8ceefcd49c726553209b3a0da
DESTINATION=${1:-"$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)/distribution/src/main/distribution/install/windows/WinSW-x64.exe"}
URI="https://github.com/winsw/winsw/releases/download/v${VERSION}/WinSW-x64.exe"

verify() {
  [ -f "$1" ] || return 1
  [ "$(wc -c < "$1" | tr -d ' ')" = "$EXPECTED_LENGTH" ] || return 1
  if command -v sha256sum >/dev/null 2>&1; then
    actual=$(sha256sum "$1" | awk '{print $1}')
  else
    actual=$(shasum -a 256 "$1" | awk '{print $1}')
  fi
  [ "$actual" = "$EXPECTED_SHA256" ]
}

if verify "$DESTINATION"; then
  printf 'Verified cached WinSW %s at %s\n' "$VERSION" "$DESTINATION"
  exit 0
fi

mkdir -p "$(dirname -- "$DESTINATION")"
temporary="${DESTINATION}.download.$$"
trap 'rm -f "$temporary"' EXIT HUP INT TERM
curl --fail --location --proto '=https' --tlsv1.2 --output "$temporary" "$URI"
verify "$temporary" || {
  printf 'Downloaded WinSW %s did not match the pinned size and SHA-256\n' "$VERSION" >&2
  exit 1
}
mv "$temporary" "$DESTINATION"
trap - EXIT HUP INT TERM
printf 'Prepared verified WinSW %s at %s\n' "$VERSION" "$DESTINATION"
