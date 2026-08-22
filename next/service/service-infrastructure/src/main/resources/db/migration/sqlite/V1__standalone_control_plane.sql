CREATE TABLE workspace (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE account_definition (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    name TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    provider_version TEXT NOT NULL,
    configuration_json TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (workspace_id, name)
);

CREATE INDEX idx_account_workspace ON account_definition(workspace_id, created_at DESC);

CREATE TABLE message_definition (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    name TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    provider_version TEXT NOT NULL,
    current_revision INTEGER NOT NULL CHECK (current_revision >= 1),
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (workspace_id, name)
);

CREATE TABLE message_revision (
    message_id TEXT NOT NULL REFERENCES message_definition(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    revision INTEGER NOT NULL CHECK (revision >= 1),
    schema_version TEXT NOT NULL,
    content_json TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (message_id, revision)
);

CREATE INDEX idx_message_workspace ON message_definition(workspace_id, created_at DESC);

CREATE TABLE audience_definition (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    name TEXT NOT NULL,
    current_snapshot_id TEXT NOT NULL,
    current_revision INTEGER NOT NULL CHECK (current_revision >= 1),
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (workspace_id, name)
);

CREATE TABLE audience_snapshot (
    id TEXT PRIMARY KEY,
    audience_id TEXT NOT NULL REFERENCES audience_definition(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    revision INTEGER NOT NULL CHECK (revision >= 1),
    record_count INTEGER NOT NULL CHECK (record_count >= 0),
    content_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (audience_id, revision)
);

CREATE TABLE audience_recipient (
    snapshot_id TEXT NOT NULL REFERENCES audience_snapshot(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    sequence INTEGER NOT NULL CHECK (sequence >= 0),
    item_id TEXT NOT NULL,
    fields_json TEXT NOT NULL,
    PRIMARY KEY (snapshot_id, sequence),
    UNIQUE (snapshot_id, item_id)
);

CREATE INDEX idx_audience_workspace ON audience_definition(workspace_id, created_at DESC);

CREATE TABLE job_definition (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    name TEXT NOT NULL,
    account_id TEXT NOT NULL REFERENCES account_definition(id),
    message_id TEXT NOT NULL REFERENCES message_definition(id),
    audience_id TEXT NOT NULL REFERENCES audience_definition(id),
    policies_json TEXT NOT NULL,
    enabled INTEGER NOT NULL CHECK (enabled IN (0, 1)),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (workspace_id, name)
);

CREATE INDEX idx_job_workspace ON job_definition(workspace_id, created_at DESC);

CREATE TABLE run_instance (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    job_id TEXT NOT NULL REFERENCES job_definition(id),
    status TEXT NOT NULL,
    state_reason TEXT NOT NULL DEFAULT '',
    dry_run INTEGER NOT NULL CHECK (dry_run IN (0, 1)),
    total INTEGER NOT NULL DEFAULT 0 CHECK (total >= 0),
    succeeded INTEGER NOT NULL DEFAULT 0 CHECK (succeeded >= 0),
    failed INTEGER NOT NULL DEFAULT 0 CHECK (failed >= 0),
    unknown_count INTEGER NOT NULL DEFAULT 0 CHECK (unknown_count >= 0),
    unsent INTEGER NOT NULL DEFAULT 0 CHECK (unsent >= 0),
    skipped INTEGER NOT NULL DEFAULT 0 CHECK (skipped >= 0),
    retried INTEGER NOT NULL DEFAULT 0 CHECK (retried >= 0),
    created_at TEXT NOT NULL,
    started_at TEXT,
    ended_at TEXT,
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE INDEX idx_run_workspace ON run_instance(workspace_id, created_at DESC);
CREATE INDEX idx_run_status ON run_instance(workspace_id, status, created_at);

CREATE TABLE run_snapshot (
    id TEXT PRIMARY KEY,
    run_id TEXT NOT NULL UNIQUE REFERENCES run_instance(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    provider_id TEXT NOT NULL,
    provider_version TEXT NOT NULL,
    account_configuration_json TEXT NOT NULL,
    message_content_json TEXT NOT NULL,
    policies_json TEXT NOT NULL,
    audience_snapshot_id TEXT NOT NULL REFERENCES audience_snapshot(id),
    content_hash TEXT NOT NULL
);

CREATE TABLE run_event (
    run_id TEXT NOT NULL REFERENCES run_instance(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    sequence INTEGER NOT NULL CHECK (sequence >= 1),
    type TEXT NOT NULL,
    occurred_at TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    severity TEXT NOT NULL,
    PRIMARY KEY (run_id, sequence)
);

CREATE INDEX idx_run_event_workspace ON run_event(workspace_id, run_id, sequence);

CREATE TABLE idempotency_record (
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    scope TEXT NOT NULL,
    key_hash TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    response_status INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    PRIMARY KEY (workspace_id, scope, key_hash)
);

INSERT INTO workspace (id, name, status, created_at, version)
VALUES ('ws_default', 'Default Workspace', 'ACTIVE', '2026-01-01T00:00:00Z', 0);
