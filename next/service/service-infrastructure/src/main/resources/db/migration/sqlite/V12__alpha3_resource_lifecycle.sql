ALTER TABLE job_definition ADD COLUMN archived INTEGER NOT NULL DEFAULT 0 CHECK (archived IN (0, 1));

ALTER TABLE run_instance ADD COLUMN source_run_id TEXT REFERENCES run_instance(id);
ALTER TABLE run_instance ADD COLUMN retry_states TEXT NOT NULL DEFAULT '';

CREATE TABLE run_retry_item (
    run_id TEXT NOT NULL REFERENCES run_instance(id),
    workspace_id TEXT NOT NULL REFERENCES workspace(id),
    item_id TEXT NOT NULL,
    PRIMARY KEY (run_id, item_id)
);

CREATE INDEX idx_run_retry_source ON run_instance(workspace_id, source_run_id, created_at DESC);
CREATE INDEX idx_run_retry_item_workspace ON run_retry_item(workspace_id, run_id, item_id);

CREATE INDEX idx_account_page ON account_definition(workspace_id, created_at DESC, id DESC);
CREATE INDEX idx_message_page ON message_definition(workspace_id, created_at DESC, id DESC);
CREATE INDEX idx_audience_page ON audience_definition(workspace_id, created_at DESC, id DESC);
CREATE INDEX idx_job_page ON job_definition(workspace_id, created_at DESC, id DESC);
CREATE INDEX idx_run_page ON run_instance(workspace_id, created_at DESC, id DESC);
CREATE INDEX idx_schedule_page ON schedule_definition(workspace_id, created_at DESC, id DESC);
