#!/bin/sh
set -eu
umask 077
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SECRETS="$HERE/secrets"
ENV_FILE="$HERE/.env.server.local"
command -v openssl >/dev/null 2>&1 || { echo "OpenSSL is required" >&2; exit 1; }
[ ! -e "$SECRETS" ] || { echo "refusing to overwrite $SECRETS" >&2; exit 1; }
[ ! -e "$ENV_FILE" ] || { echo "refusing to overwrite $ENV_FILE" >&2; exit 1; }
mkdir -m 0700 "$SECRETS"

openssl ecparam -name prime256v1 -genkey -noout -out "$SECRETS/agent-ca-key.pem"
openssl req -x509 -new -sha256 -days 3650 -key "$SECRETS/agent-ca-key.pem" \
  -subj "/CN=WePush Next Development Agent CA" -out "$SECRETS/agent-ca.pem"
openssl ecparam -name prime256v1 -genkey -noout -out "$SECRETS/grpc-server-key.pem"
openssl req -new -sha256 -key "$SECRETS/grpc-server-key.pem" \
  -subj "/CN=wepush-next-service" -out "$SECRETS/grpc-server.csr"
cat > "$SECRETS/grpc-server.ext" <<'EOF'
subjectAltName=DNS:localhost,DNS:service-1,DNS:service-2,IP:127.0.0.1
extendedKeyUsage=serverAuth
keyUsage=digitalSignature,keyEncipherment
EOF
openssl x509 -req -sha256 -days 825 -in "$SECRETS/grpc-server.csr" \
  -CA "$SECRETS/agent-ca.pem" -CAkey "$SECRETS/agent-ca-key.pem" -CAcreateserial \
  -extfile "$SECRETS/grpc-server.ext" -out "$SECRETS/grpc-server-cert.pem"
rm -f "$SECRETS/grpc-server.csr" "$SECRETS/grpc-server.ext" \
  "$SECRETS/agent-ca.srl" "$SECRETS/agent-ca.pem.srl"

cat > "$ENV_FILE" <<EOF
POSTGRES_PASSWORD=$(openssl rand -hex 24)
MINIO_ROOT_USER=wepush-minio
MINIO_ROOT_PASSWORD=$(openssl rand -hex 24)
WEPUSH_BOOTSTRAP_TOKEN=$(openssl rand -hex 32)
WEPUSH_MASTER_KEY_BASE64=$(openssl rand -base64 32 | tr -d '\n')
WEPUSH_AGENT_ARTIFACT_SIGNING_KEY_BASE64=$(openssl rand -base64 32 | tr -d '\n')
WEPUSH_AGENT_PUBLIC_BASE_URL=http://127.0.0.1:18990
EOF
chmod 0600 "$ENV_FILE" "$SECRETS"/*
echo "Created development secrets and $ENV_FILE"
