CREATE TABLE artifact_multipart_upload (
    artifact_id TEXT PRIMARY KEY REFERENCES artifact_record(id) ON DELETE CASCADE,
    workspace_id TEXT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    upload_id TEXT NOT NULL,
    part_size INTEGER NOT NULL CHECK (part_size >= 5242880),
    part_count INTEGER NOT NULL CHECK (part_count BETWEEN 1 AND 10000),
    created_at TEXT NOT NULL
);

CREATE INDEX idx_artifact_multipart_workspace
    ON artifact_multipart_upload(workspace_id, created_at);
