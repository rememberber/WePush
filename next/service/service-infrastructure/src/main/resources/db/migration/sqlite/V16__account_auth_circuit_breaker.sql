CREATE TABLE account_auth_circuit (
    workspace_id TEXT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    account_id TEXT NOT NULL REFERENCES account_definition(id) ON DELETE CASCADE,
    failure_runs INTEGER NOT NULL DEFAULT 0 CHECK (failure_runs >= 0),
    first_failure_at TEXT,
    last_failure_at TEXT,
    open_until TEXT,
    last_run_id TEXT NOT NULL DEFAULT '',
    version INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (workspace_id, account_id)
);

CREATE TABLE account_auth_failure_run (
    workspace_id TEXT NOT NULL,
    account_id TEXT NOT NULL,
    run_id TEXT NOT NULL REFERENCES run_instance(id) ON DELETE CASCADE,
    detected_at TEXT NOT NULL,
    PRIMARY KEY (workspace_id, account_id, run_id),
    FOREIGN KEY (workspace_id, account_id)
        REFERENCES account_auth_circuit(workspace_id, account_id) ON DELETE CASCADE
);

CREATE INDEX idx_account_auth_circuit_open
    ON account_auth_circuit(workspace_id, open_until);
