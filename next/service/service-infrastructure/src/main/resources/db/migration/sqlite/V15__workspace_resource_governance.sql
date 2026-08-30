CREATE TABLE workspace_policy (
    workspace_id TEXT PRIMARY KEY REFERENCES workspace(id) ON DELETE CASCADE,
    max_agents INTEGER NOT NULL DEFAULT 0 CHECK (max_agents >= 0),
    max_concurrent_runs INTEGER NOT NULL DEFAULT 0 CHECK (max_concurrent_runs >= 0),
    max_total_concurrency INTEGER NOT NULL DEFAULT 0 CHECK (max_total_concurrency >= 0),
    artifact_quota_bytes INTEGER NOT NULL DEFAULT 0 CHECK (artifact_quota_bytes >= 0),
    artifact_retention_seconds INTEGER NOT NULL DEFAULT 604800 CHECK (artifact_retention_seconds >= 300),
    updated_at TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);

INSERT INTO workspace_policy
    (workspace_id, max_agents, max_concurrent_runs, max_total_concurrency,
     artifact_quota_bytes, artifact_retention_seconds, updated_at, version)
SELECT id, 0, 0, 0, 0, 604800, '2026-08-29T00:00:00Z', 0 FROM workspace;

CREATE TABLE workspace_run_reservation (
    run_id TEXT PRIMARY KEY REFERENCES run_instance(id) ON DELETE CASCADE,
    workspace_id TEXT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    target_concurrency INTEGER NOT NULL CHECK (target_concurrency > 0),
    created_at TEXT NOT NULL
);

CREATE INDEX idx_workspace_run_reservation_usage
    ON workspace_run_reservation(workspace_id, created_at);
