CREATE TABLE agent_message_outbox (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    run_id TEXT NOT NULL REFERENCES run_instance(id),
    agent_id TEXT NOT NULL,
    lease_id TEXT NOT NULL REFERENCES agent_lease(id),
    message_type TEXT NOT NULL,
    command_type TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TEXT NOT NULL,
    next_attempt_at TEXT NOT NULL,
    delivered_at TEXT,
    acknowledged_at TEXT,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    last_error TEXT NOT NULL
);

CREATE INDEX idx_agent_outbox_pending
    ON agent_message_outbox(acknowledged_at, next_attempt_at, agent_id);
CREATE INDEX idx_agent_outbox_lease ON agent_message_outbox(lease_id, message_type);
