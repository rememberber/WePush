CREATE TABLE agent_enrollment_token (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    used_at TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX idx_agent_enrollment_expiry
    ON agent_enrollment_token(expires_at, used_at);

CREATE TABLE agent_credential (
    id TEXT PRIMARY KEY,
    agent_id TEXT NOT NULL,
    token_hash TEXT NOT NULL,
    certificate_fingerprint TEXT NOT NULL DEFAULT '',
    expires_at TEXT NOT NULL,
    revoked_at TEXT,
    created_at TEXT NOT NULL,
    last_used_at TEXT
);

CREATE INDEX idx_agent_credential_agent
    ON agent_credential(agent_id, revoked_at, expires_at);
