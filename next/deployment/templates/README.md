# WePush Next self-host templates

These templates extend the production-oriented `deployment/container/compose.server.yaml` baseline.
They intentionally contain no usable credentials. Copy the relevant example, replace every
`REPLACE_ME`/hostname, and keep PostgreSQL, S3 and Agent gRPC on private networks.

- `nginx/wepush-next.conf`: TLS reverse proxy for the HTTP API/Web UI and long-lived SSE streams.
- `traefik/compose.override.yaml`: labels for an existing Traefik v3 deployment.
- `kubernetes/`: Service Deployment, internal Services, ingress and Pod disruption budget.

Server mode still requires PostgreSQL, S3, API security and Agent gRPC mTLS. Following the controlled
upgrade guide, let one Service instance finish Flyway migration before rolling the remaining pods; verify `/actuator/health/readiness` and
`/actuator/health/installation` before sending traffic.
