CREATE TABLE agent_registration (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    agent_version TEXT NOT NULL,
    protocol_version INTEGER NOT NULL CHECK (protocol_version >= 1),
    os_name TEXT NOT NULL,
    architecture TEXT NOT NULL,
    java_version TEXT NOT NULL,
    maximum_runs INTEGER NOT NULL CHECK (maximum_runs >= 1),
    active_runs INTEGER NOT NULL CHECK (active_runs >= 0),
    available_runs INTEGER NOT NULL CHECK (available_runs >= 0),
    providers_json TEXT NOT NULL,
    session_id TEXT NOT NULL,
    last_agent_sequence INTEGER NOT NULL CHECK (last_agent_sequence >= 1),
    last_service_sequence INTEGER NOT NULL CHECK (last_service_sequence >= 1),
    connected_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL,
    disconnected_at TEXT,
    version INTEGER NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE INDEX idx_agent_status_seen ON agent_registration(status, last_seen_at);
