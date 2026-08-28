CREATE TABLE wepush_release_compatibility (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    compatibility_line TEXT NOT NULL,
    minimum_upgrade_version TEXT NOT NULL,
    minimum_rollback_version TEXT NOT NULL,
    established_at TEXT NOT NULL
);

INSERT INTO wepush_release_compatibility
    (id, compatibility_line, minimum_upgrade_version, minimum_rollback_version, established_at)
VALUES
    (1, '1.x', '0.1.0-beta.1', '0.1.0-beta.1', '2026-08-29T00:00:00Z');
