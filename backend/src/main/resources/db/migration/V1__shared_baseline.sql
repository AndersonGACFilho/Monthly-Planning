-- Baseline of the schema. Each module adds its tables in later migrations:
-- V<n>__<module>_<description>.sql (IMP-03).
CREATE TABLE schema_info (
    id         INTEGER PRIMARY KEY CHECK (id = 1),
    created_at TEXT    NOT NULL
);
INSERT INTO schema_info (id, created_at) VALUES (1, strftime('%Y-%m-%dT%H:%M:%SZ', 'now'));
