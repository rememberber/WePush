CREATE TABLE audience_import_session (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    audience_id TEXT REFERENCES audience_definition(id),
    name TEXT NOT NULL,
    format TEXT NOT NULL,
    item_id_column TEXT NOT NULL,
    field_mapping_json TEXT NOT NULL,
    status TEXT NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0 CHECK (total_rows >= 0),
    accepted_rows INTEGER NOT NULL DEFAULT 0 CHECK (accepted_rows >= 0),
    rejected_rows INTEGER NOT NULL DEFAULT 0 CHECK (rejected_rows >= 0),
    duplicate_rows INTEGER NOT NULL DEFAULT 0 CHECK (duplicate_rows >= 0),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE audience_import_row (
    import_id TEXT NOT NULL REFERENCES audience_import_session(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    sequence INTEGER NOT NULL CHECK (sequence >= 1),
    item_id TEXT NOT NULL,
    fields_json TEXT NOT NULL,
    raw_line TEXT NOT NULL,
    accepted INTEGER NOT NULL CHECK (accepted IN (0, 1)),
    error_code TEXT NOT NULL,
    error_message TEXT NOT NULL,
    PRIMARY KEY (import_id, sequence)
);

CREATE INDEX idx_audience_import_rows ON audience_import_row(workspace_id, import_id, accepted, sequence);
CREATE INDEX idx_audience_import_item ON audience_import_row(import_id, item_id, sequence);
