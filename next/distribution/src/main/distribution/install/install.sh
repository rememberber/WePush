#!/bin/sh
set -eu
COMPONENT=${1:-standalone}
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
case "$(uname -s)" in
  Linux) "$ROOT/linux/install.sh" "$COMPONENT" ;;
  Darwin) "$ROOT/macos/install.sh" "$COMPONENT" ;;
  *) echo "Unsupported operating system; use install.ps1 on Windows" >&2; exit 1 ;;
esac
case "$COMPONENT" in standalone|service|all)
  "$ROOT/verify-install.sh" "$(basename "$(dirname "$ROOT")")" ;;
esac
