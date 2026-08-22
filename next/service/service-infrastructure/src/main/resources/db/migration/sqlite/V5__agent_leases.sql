CREATE TABLE agent_lease (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    run_id TEXT NOT NULL REFERENCES run_instance(id),
    agent_id TEXT NOT NULL REFERENCES agent_registration(id),
    agent_session_id TEXT NOT NULL,
    epoch INTEGER NOT NULL CHECK (epoch >= 1),
    fencing_token TEXT NOT NULL,
    status TEXT NOT NULL,
    assigned_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    acknowledged_at TEXT,
    completed_at TEXT,
    last_event_sequence INTEGER NOT NULL DEFAULT 0 CHECK (last_event_sequence >= 0),
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (workspace_id, run_id, epoch),
    UNIQUE (id, fencing_token)
);

CREATE UNIQUE INDEX idx_agent_lease_active_run
    ON agent_lease(workspace_id, run_id)
    WHERE status IN ('OFFERED', 'ACKNOWLEDGED', 'RUNNING');

CREATE INDEX idx_agent_lease_agent_status
    ON agent_lease(agent_id, status, expires_at);
