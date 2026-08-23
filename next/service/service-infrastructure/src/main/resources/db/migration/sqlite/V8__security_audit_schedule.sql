CREATE TABLE api_principal (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE api_token (
    id TEXT PRIMARY KEY,
    principal_id TEXT NOT NULL REFERENCES api_principal(id),
    name TEXT NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TEXT NOT NULL,
    revoked_at TEXT,
    created_at TEXT NOT NULL,
    last_used_at TEXT
);

CREATE INDEX idx_api_token_expiry ON api_token(expires_at, revoked_at);

CREATE TABLE role_binding (
    principal_id TEXT NOT NULL REFERENCES api_principal(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    role TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (principal_id, workspace_id)
);

CREATE TABLE audit_event (
    id TEXT PRIMARY KEY,
    workspace_id TEXT,
    actor_type TEXT NOT NULL,
    actor_id TEXT NOT NULL,
    action TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    result TEXT NOT NULL,
    details_json TEXT NOT NULL,
    occurred_at TEXT NOT NULL
);

CREATE INDEX idx_audit_workspace_time ON audit_event(workspace_id, occurred_at DESC, id);
CREATE INDEX idx_audit_actor_time ON audit_event(actor_id, occurred_at DESC, id);

CREATE TABLE schedule_definition (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    job_id TEXT NOT NULL REFERENCES job_definition(id),
    name TEXT NOT NULL,
    cron_expression TEXT NOT NULL,
    timezone TEXT NOT NULL,
    misfire_policy TEXT NOT NULL,
    enabled INTEGER NOT NULL CHECK (enabled IN (0, 1)),
    next_fire_at TEXT NOT NULL,
    last_fire_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (workspace_id, name)
);

CREATE INDEX idx_schedule_due ON schedule_definition(enabled, next_fire_at, id);
