#!/usr/bin/env bash
set -euo pipefail

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
next_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
cd "$next_root"

version=$(awk '
  match($0, /<version>[^<]+<\/version>/) {
    value = substr($0, RSTART + 9, RLENGTH - 19)
    print value
    exit
  }
' pom.xml)

if [[ -z "$version" ]]; then
  echo "Cannot read the release version from next/pom.xml" >&2
  exit 1
fi

expected_tag="next-v$version"
actual_tag=${1:-$expected_tag}
if [[ "$actual_tag" != "$expected_tag" ]]; then
  echo "Release tag must be $expected_tag, got $actual_tag" >&2
  exit 1
fi

while IFS= read -r pom; do
  if ! grep -q "<version>$version</version>" "$pom"; then
    echo "Maven module is not aligned to $version: $pom" >&2
    exit 1
  fi
done < <(find . -name pom.xml -not -path '*/target/*' -print | sort)

ui_packages=(
  ui/package.json
  ui/apps/web/package.json
  ui/apps/desktop/package.json
  ui/packages/api-client/package.json
  ui/packages/design-tokens/package.json
  ui/packages/features/package.json
  ui/packages/schema-renderer/package.json
  ui/packages/ui/package.json
)
for package_file in "${ui_packages[@]}"; do
  package_version=$(node -p "require('./$package_file').version")
  if [[ "$package_version" != "$version" ]]; then
    echo "UI package is not aligned to $version: $package_file ($package_version)" >&2
    exit 1
  fi
done

grep -q "version: $version" service/service-api/src/main/resources/openapi/openapi.yaml
grep -q "当前版本：\`$version\`" README.md

required_files=(
  PREVIEW-NOTICE.md
  CHANGELOG.md
  SECURITY.md
  THIRD-PARTY-NOTICES.md
  sdk/README.md
  "docs/releases/$version.md"
)
for required_file in "${required_files[@]}"; do
  if [[ ! -s "$required_file" ]]; then
    echo "Required release document is missing: $required_file" >&2
    exit 1
  fi
done

if find . -name pom.xml -not -path '*/target/*' -exec grep -H 'SNAPSHOT' {} + | grep -q .; then
  echo "A Maven SNAPSHOT remains in the release reactor" >&2
  exit 1
fi
if grep -q '0.1.0-SNAPSHOT' docs/deployment-and-operations.md; then
  echo "Deployment documentation still references the old SNAPSHOT" >&2
  exit 1
fi

echo "WePush Next release metadata is aligned: $expected_tag"
