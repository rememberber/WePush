CREATE TABLE secret_record (
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    secret_namespace TEXT NOT NULL,
    secret_name TEXT NOT NULL,
    secret_version TEXT NOT NULL,
    record_version INTEGER NOT NULL CHECK (record_version >= 1),
    algorithm TEXT NOT NULL,
    key_version TEXT NOT NULL,
    ciphertext BLOB NOT NULL,
    data_nonce BLOB NOT NULL,
    encrypted_dek BLOB NOT NULL,
    dek_nonce BLOB NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    PRIMARY KEY (workspace_id, secret_namespace, secret_name, secret_version)
);

CREATE INDEX idx_secret_workspace ON secret_record(workspace_id, secret_namespace, secret_name);

CREATE TABLE run_item_result (
    run_id TEXT NOT NULL REFERENCES run_instance(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    item_id TEXT NOT NULL,
    attempts INTEGER NOT NULL CHECK (attempts >= 0),
    state TEXT NOT NULL,
    provider_code TEXT NOT NULL,
    diagnostic TEXT NOT NULL,
    external_request_id TEXT NOT NULL,
    completed_at TEXT NOT NULL,
    metadata_json TEXT NOT NULL,
    PRIMARY KEY (run_id, item_id)
);

CREATE INDEX idx_run_item_result_page ON run_item_result(workspace_id, run_id, completed_at, item_id);

CREATE TABLE run_command (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    run_id TEXT NOT NULL REFERENCES run_instance(id),
    type TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    status TEXT NOT NULL,
    result_code TEXT NOT NULL,
    result_message TEXT NOT NULL,
    created_at TEXT NOT NULL,
    acknowledged_at TEXT,
    UNIQUE (workspace_id, run_id, id)
);

CREATE INDEX idx_run_command_run ON run_command(workspace_id, run_id, created_at, id);
