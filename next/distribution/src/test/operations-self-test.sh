#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PLATFORM=$(uname -s)
case "$PLATFORM" in
  Linux) PLATFORM_DIR=linux; HASH=sha256sum ;;
  Darwin) PLATFORM_DIR=macos; HASH='shasum -a 256' ;;
  *) echo "Unsupported test platform: $PLATFORM" >&2; exit 2 ;;
esac
TOOLS="$ROOT/main/distribution/install/$PLATFORM_DIR"
TEMP=$(mktemp -d "${TMPDIR:-/tmp}/wepush-next-operations.XXXXXX")
cleanup() { rm -rf "$TEMP"; }
trap cleanup EXIT HUP INT TERM

CONFIG_ROOT="$TEMP/config"
DATA_ROOT="$TEMP/data"
INSTALL_ROOT="$TEMP/install"
BACKUP_ROOT="$TEMP/backups"
EXPECTED="$TEMP/expected"
mkdir -p "$CONFIG_ROOT" "$DATA_ROOT/service/secrets" "$DATA_ROOT/service/artifacts/nested" \
  "$DATA_ROOT/agent/plugins/active" "$INSTALL_ROOT/releases/0.1.0-beta.1" "$BACKUP_ROOT"
printf 'WEPUSH_MODE=standalone\n' > "$CONFIG_ROOT/service.env"
printf 'WEPUSH_PLUGIN_TRUSTED_KEYS=test-key\n' > "$CONFIG_ROOT/agent.env"
printf 'sqlite-database\n' > "$DATA_ROOT/service/wepush-next.db"
printf 'master-key\n' > "$DATA_ROOT/service/secrets/master-key.json"
printf 'artifact\n' > "$DATA_ROOT/service/artifacts/nested/audience.csv"
printf 'agent-identity\n' > "$DATA_ROOT/agent/identity.json"
printf 'agent-journal\n' > "$DATA_ROOT/agent/journal.json"
printf 'event-outbox\n' > "$DATA_ROOT/agent/event-outbox"
printf 'completion-outbox\n' > "$DATA_ROOT/agent/completion-outbox"
printf 'signed-plugin\n' > "$DATA_ROOT/agent/plugins/active/provider.zip"
ln -s "$INSTALL_ROOT/releases/0.1.0-beta.1" "$INSTALL_ROOT/current"
mkdir -p "$EXPECTED"
cp -a "$CONFIG_ROOT" "$EXPECTED/config"
cp -a "$DATA_ROOT" "$EXPECTED/data"

export WEPUSH_ALLOW_NON_ROOT=true WEPUSH_SKIP_SERVICE_CONTROL=true WEPUSH_SKIP_HEALTH_CHECK=true
export WEPUSH_CONFIG_ROOT="$CONFIG_ROOT" WEPUSH_DATA_ROOT="$DATA_ROOT"
export WEPUSH_INSTALL_ROOT="$INSTALL_ROOT" WEPUSH_BACKUP_ROOT="$BACKUP_ROOT"

ARCHIVE=$("$TOOLS/backup.sh" "$BACKUP_ROOT" | tail -1)
"$TOOLS/restore.sh" --validate-only "$ARCHIVE"
printf 'corrupted-current-data\n' > "$DATA_ROOT/service/wepush-next.db"
rm -f "$DATA_ROOT/agent/identity.json" "$CONFIG_ROOT/agent.env"
"$TOOLS/restore.sh" "$ARCHIVE"
diff -ru "$EXPECTED/config" "$CONFIG_ROOT"
diff -ru "$EXPECTED/data" "$DATA_ROOT"

CORRUPT_ROOT="$TEMP/corrupt"
mkdir -p "$CORRUPT_ROOT"
tar -xzf "$ARCHIVE" -C "$CORRUPT_ROOT"
printf 'tampered\n' >> "$CORRUPT_ROOT/payload/data/service/wepush-next.db"
CORRUPT="$TEMP/corrupt.tar.gz"
tar -C "$CORRUPT_ROOT" -czf "$CORRUPT" BACKUP-MANIFEST SHA256SUMS payload
if "$TOOLS/restore.sh" --validate-only "$CORRUPT" >/dev/null 2>&1; then
  echo "corrupted backup unexpectedly validated" >&2
  exit 1
fi

EXTRA_ROOT="$TEMP/extra"
mkdir -p "$EXTRA_ROOT"
tar -xzf "$ARCHIVE" -C "$EXTRA_ROOT"
printf 'unlisted\n' > "$EXTRA_ROOT/payload/data/unlisted.txt"
EXTRA="$TEMP/extra.tar.gz"
tar -C "$EXTRA_ROOT" -czf "$EXTRA" BACKUP-MANIFEST SHA256SUMS payload
if "$TOOLS/restore.sh" --validate-only "$EXTRA" >/dev/null 2>&1; then
  echo "backup with an unlisted payload unexpectedly validated" >&2
  exit 1
fi

UPGRADE_ROOT="$TEMP/upgrade/wepush-next-0.1.0-test"
mkdir -p "$UPGRADE_ROOT/install/$PLATFORM_DIR" "$UPGRADE_ROOT/install"
cat > "$UPGRADE_ROOT/install/$PLATFORM_DIR/install.sh" <<'EOF'
#!/bin/sh
set -eu
mkdir -p "$WEPUSH_INSTALL_ROOT/releases/0.1.0-test"
ln -sfn "$WEPUSH_INSTALL_ROOT/releases/0.1.0-test" "$WEPUSH_INSTALL_ROOT/current.new"
if [ -L "$WEPUSH_INSTALL_ROOT/current" ]; then rm "$WEPUSH_INSTALL_ROOT/current"; fi
mv "$WEPUSH_INSTALL_ROOT/current.new" "$WEPUSH_INSTALL_ROOT/current"
printf 'upgrade-mutated-data\n' > "$WEPUSH_DATA_ROOT/service/wepush-next.db"
EOF
cat > "$UPGRADE_ROOT/install/verify-install.sh" <<'EOF'
#!/bin/sh
exit 1
EOF
chmod +x "$UPGRADE_ROOT/install/$PLATFORM_DIR/install.sh" "$UPGRADE_ROOT/install/verify-install.sh"
UPGRADE="$TEMP/upgrade.tar.gz"
tar -C "$TEMP/upgrade" -czf "$UPGRADE" "$(basename "$UPGRADE_ROOT")"
UPGRADE_SHA=$(sh -c "$HASH \"$UPGRADE\"" | awk '{print $1}')
if "$TOOLS/upgrade.sh" "$UPGRADE" "$UPGRADE_SHA" >/dev/null 2>&1; then
  echo "forced failed upgrade unexpectedly succeeded" >&2
  exit 1
fi
[ "$(readlink "$INSTALL_ROOT/current")" = "$INSTALL_ROOT/releases/0.1.0-beta.1" ]
diff -ru "$EXPECTED/config" "$CONFIG_ROOT"
diff -ru "$EXPECTED/data" "$DATA_ROOT"

SUCCESS_ROOT="$TEMP/success/wepush-next-1.0.0"
mkdir -p "$SUCCESS_ROOT/install/$PLATFORM_DIR" "$SUCCESS_ROOT/install"
cat > "$SUCCESS_ROOT/install/$PLATFORM_DIR/install.sh" <<'EOF'
#!/bin/sh
set -eu
mkdir -p "$WEPUSH_INSTALL_ROOT/releases/1.0.0"
ln -sfn "$WEPUSH_INSTALL_ROOT/releases/1.0.0" "$WEPUSH_INSTALL_ROOT/current.new"
if [ -L "$WEPUSH_INSTALL_ROOT/current" ]; then rm "$WEPUSH_INSTALL_ROOT/current"; fi
mv "$WEPUSH_INSTALL_ROOT/current.new" "$WEPUSH_INSTALL_ROOT/current"
EOF
cat > "$SUCCESS_ROOT/install/verify-install.sh" <<'EOF'
#!/bin/sh
set -eu
[ "$1" = 1.0.0 ]
EOF
chmod +x "$SUCCESS_ROOT/install/$PLATFORM_DIR/install.sh" "$SUCCESS_ROOT/install/verify-install.sh"
SUCCESS="$TEMP/success.tar.gz"
tar -C "$TEMP/success" -czf "$SUCCESS" "$(basename "$SUCCESS_ROOT")"
SUCCESS_SHA=$(sh -c "$HASH \"$SUCCESS\"" | awk '{print $1}')
"$TOOLS/upgrade.sh" "$SUCCESS" "$SUCCESS_SHA"
[ "$(readlink "$INSTALL_ROOT/current")" = "$INSTALL_ROOT/releases/1.0.0" ]
diff -ru "$EXPECTED/config" "$CONFIG_ROOT"
diff -ru "$EXPECTED/data" "$DATA_ROOT"

"$TOOLS/uninstall.sh"
[ ! -e "$INSTALL_ROOT/current" ]
[ -f "$CONFIG_ROOT/service.env" ]
[ -f "$DATA_ROOT/service/wepush-next.db" ]
mkdir -p "$INSTALL_ROOT/releases/1.0.0"
"$TOOLS/uninstall.sh" --purge
[ ! -e "$INSTALL_ROOT" ]
[ ! -e "$CONFIG_ROOT" ]
[ ! -e "$DATA_ROOT" ]

echo "Backup, beta.1 upgrade, rollback, uninstall and purge passed on $PLATFORM_DIR"
