-- SQLite refuses ADD COLUMN when the new column combines REFERENCES with a
-- non-NULL default. Rebuild the small enrollment table so upgrades preserve
-- outstanding tokens and the foreign key is actually installed. These
-- statements are also valid on PostgreSQL, which keeps both database modes on
-- the same ordered migration history.
ALTER TABLE agent_enrollment_token RENAME TO agent_enrollment_token_v7;
DROP INDEX idx_agent_enrollment_expiry;

CREATE TABLE agent_enrollment_token (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    token_hash TEXT NOT NULL,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    expires_at TEXT NOT NULL,
    used_at TEXT,
    created_at TEXT NOT NULL
);

INSERT INTO agent_enrollment_token
    (id, name, token_hash, workspace_id, expires_at, used_at, created_at)
SELECT id, name, token_hash, 'ws_default', expires_at, used_at, created_at
FROM agent_enrollment_token_v7;

DROP TABLE agent_enrollment_token_v7;

CREATE INDEX idx_agent_enrollment_expiry
    ON agent_enrollment_token(expires_at, used_at);

CREATE TABLE agent_workspace_binding (
    agent_id TEXT NOT NULL,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    created_at TEXT NOT NULL,
    PRIMARY KEY (agent_id, workspace_id)
);

CREATE INDEX idx_agent_workspace_workspace ON agent_workspace_binding(workspace_id, agent_id);
