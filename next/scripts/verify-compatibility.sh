#!/usr/bin/env bash
set -euo pipefail

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
next_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
cd "$next_root"

hash_stream() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum | awk '{print $1}'
  else
    shasum -a 256 | awk '{print $1}'
  fi
}

expected_openapi=$(tr -d '[:space:]' < compatibility/0.1.0-beta.1-openapi.sha256)
actual_openapi=$(sed -E 's/^  version: .*/  version: __VERSION__/' \
  service/service-api/src/main/resources/openapi/openapi.yaml | hash_stream)
if [[ "$actual_openapi" != "$expected_openapi" ]]; then
  echo "The public OpenAPI contract differs from the 0.1.0-beta.1 compatibility baseline" >&2
  echo "Expected normalized SHA-256: $expected_openapi" >&2
  echo "Actual normalized SHA-256:   $actual_openapi" >&2
  exit 1
fi

current_keys=$(mktemp "${TMPDIR:-/tmp}/wepush-next-config-keys.XXXXXX")
cleanup() { rm -f "$current_keys"; }
trap cleanup EXIT HUP INT TERM
grep -Eho 'WEPUSH_[A-Z0-9_]+' service/service-app/src/main/resources/application.yaml \
  distribution/src/main/distribution/config/*.example | sort -u > "$current_keys"
while IFS= read -r required_key; do
  required_key=${required_key%$'\r'}
  [[ -z "$required_key" ]] || grep -Fxq "$required_key" "$current_keys" || {
    echo "Configuration key from 0.1.0-beta.1 is no longer accepted: $required_key" >&2
    exit 1
  }
done < compatibility/0.1.0-beta.1-config-keys.txt

echo "Beta.1 OpenAPI and configuration compatibility baselines are preserved"
