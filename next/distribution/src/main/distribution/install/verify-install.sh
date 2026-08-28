#!/bin/sh
set -eu
EXPECTED_VERSION=${1:-}
if [ "${WEPUSH_SKIP_HEALTH_CHECK:-false}" = true ]; then
  echo "Installation health check skipped by explicit test configuration"
  exit 0
fi
command -v curl >/dev/null 2>&1 || { echo "curl is required for installation verification" >&2; exit 1; }
BASE_URL=${WEPUSH_LOCAL_SERVICE_URL:-http://127.0.0.1:18990}
ATTEMPTS=${WEPUSH_HEALTH_ATTEMPTS:-60}
COUNT=0
while [ "$COUNT" -lt "$ATTEMPTS" ]; do
  INSTALLATION=$(curl --connect-timeout 2 --max-time 5 --fail --silent "$BASE_URL/actuator/health/installation" 2>/dev/null || true)
  if printf '%s' "$INSTALLATION" | grep -q '"status":"UP"' \
      && printf '%s' "$INSTALLATION" | grep -q '"databaseVersion"' \
      && printf '%s' "$INSTALLATION" | grep -q '"dryRun":"DRY_RUN"'; then
    SYSTEM=$(curl --connect-timeout 2 --max-time 5 --fail --silent "$BASE_URL/api/v1/system/info")
    if [ -n "$EXPECTED_VERSION" ] && ! printf '%s' "$SYSTEM" | grep -Fq "\"version\":\"$EXPECTED_VERSION\""; then
      echo "Service version does not match $EXPECTED_VERSION" >&2
      exit 1
    fi
    echo "Installation verified: readiness, database migration and Provider Dry Run are healthy"
    exit 0
  fi
  COUNT=$((COUNT + 1))
  sleep 1
done
echo "Installation did not become healthy at $BASE_URL" >&2
exit 1
