CREATE TABLE artifact_record (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    run_id TEXT REFERENCES run_instance(id),
    type TEXT NOT NULL,
    backend TEXT NOT NULL,
    location TEXT NOT NULL UNIQUE,
    original_name TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size INTEGER NOT NULL DEFAULT 0 CHECK (size >= 0),
    sha256 TEXT NOT NULL DEFAULT '',
    state TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    pinned INTEGER NOT NULL DEFAULT 0 CHECK (pinned IN (0, 1)),
    legal_hold INTEGER NOT NULL DEFAULT 0 CHECK (legal_hold IN (0, 1)),
    created_at TEXT NOT NULL,
    ready_at TEXT,
    deleted_at TEXT,
    last_error TEXT NOT NULL DEFAULT '',
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE INDEX idx_artifact_run ON artifact_record(workspace_id, run_id, created_at DESC);
CREATE INDEX idx_artifact_retention ON artifact_record(state, expires_at, pinned, legal_hold);
