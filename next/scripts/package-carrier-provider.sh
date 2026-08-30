#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <cmpp|smgp|sgip|smpp> <output-directory>" >&2
  exit 2
fi

PROTOCOL=$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')
case "$PROTOCOL" in
  cmpp) EXTENSION_CLASS=com.fangxuele.wepush.next.plugin.cmpp.CmppProviderExtension ;;
  smgp) EXTENSION_CLASS=com.fangxuele.wepush.next.plugin.smgp.SmgpProviderExtension ;;
  sgip) EXTENSION_CLASS=com.fangxuele.wepush.next.plugin.sgip.SgipProviderExtension ;;
  smpp) EXTENSION_CLASS=com.fangxuele.wepush.next.plugin.smpp.SmppProviderExtension ;;
  *) echo "Unsupported protocol: $1" >&2; exit 2 ;;
esac

: "${WEPUSH_PLUGIN_SIGNING_KEY_ID:?Set WEPUSH_PLUGIN_SIGNING_KEY_ID}"
: "${WEPUSH_PLUGIN_SIGNING_KEY_PKCS8_BASE64:?Set WEPUSH_PLUGIN_SIGNING_KEY_PKCS8_BASE64}"

case "$WEPUSH_PLUGIN_SIGNING_KEY_ID" in *[!A-Za-z0-9._-]*|'') echo "Signing key id is invalid" >&2; exit 2 ;; esac

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
NEXT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
OUTPUT=$(mkdir -p "$2" && CDPATH= cd -- "$2" && pwd)
STAGING=$(mktemp -d "${TMPDIR:-/tmp}/wepush-carrier-plugin.XXXXXX")
cleanup() { rm -rf "$STAGING"; }
trap cleanup EXIT HUP INT TERM

VERSION=$(awk '
  match($0, /<version>[^<]+<\/version>/) {
    value = substr($0, RSTART + 9, RLENGTH - 19)
    print value
    exit
  }
' "$NEXT_ROOT/pom.xml")
case "$VERSION" in *[!A-Za-z0-9._+-]*|'') echo "Project version is invalid" >&2; exit 2 ;; esac

mkdir -p "$STAGING/package/lib" "$STAGING/dependencies"
if [ "${WEPUSH_PLUGIN_SKIP_BUILD:-false}" != "true" ]; then
  "$NEXT_ROOT/mvnw" -q -f "$NEXT_ROOT/pom.xml" -pl "plugins/provider-$PROTOCOL" -am install
fi
PLUGIN_JAR="$NEXT_ROOT/plugins/provider-$PROTOCOL/target/provider-$PROTOCOL-$VERSION.jar"
EXTENSION_INDEX=$(unzip -p "$PLUGIN_JAR" META-INF/extensions.idx 2>/dev/null \
  | sed '/^[[:space:]]*#/d; /^[[:space:]]*$/d' || true)
if [ "$EXTENSION_INDEX" != "$EXTENSION_CLASS" ]; then
  echo "PF4J extension index is missing or invalid in $PLUGIN_JAR" >&2
  exit 1
fi
"$NEXT_ROOT/mvnw" -q -f "$NEXT_ROOT/pom.xml" -pl "plugins/provider-$PROTOCOL" \
  dependency:copy-dependencies -DincludeScope=runtime \
  -DexcludeGroupIds=com.fangxuele.wepush.next,org.pf4j,org.slf4j \
  -DoutputDirectory="$STAGING/dependencies"

cp "$PLUGIN_JAR" "$STAGING/package/lib/"
cp "$NEXT_ROOT/plugins/provider-carrier-common/target/provider-carrier-common-$VERSION.jar" "$STAGING/package/lib/"
for dependency in "$STAGING"/dependencies/*.jar; do
  [ -f "$dependency" ] && cp "$dependency" "$STAGING/package/lib/"
done
cp "$NEXT_ROOT/plugins/provider-$PROTOCOL/src/main/resources/plugin.properties" "$STAGING/package/plugin.properties"

ARCHIVE="$OUTPUT/wepush-provider-$PROTOCOL-$VERSION.zip"
java "$SCRIPT_DIR/PluginPackager.java" "$STAGING/package" "$ARCHIVE" "$PROTOCOL" "$VERSION"
