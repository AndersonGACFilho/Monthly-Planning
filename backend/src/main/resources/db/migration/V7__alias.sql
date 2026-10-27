CREATE TABLE alias (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    display_name        TEXT    NOT NULL,
    default_category_id INTEGER REFERENCES category (id),
    version             INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE alias_pattern (
    alias_id               INTEGER NOT NULL REFERENCES alias (id) ON DELETE CASCADE,
    normalized_description TEXT    NOT NULL UNIQUE,
    PRIMARY KEY (alias_id, normalized_description)
);

CREATE TABLE alias_override (
    bill_entry_id INTEGER PRIMARY KEY REFERENCES bill_entry (id) ON DELETE CASCADE,
    display_name  TEXT    NOT NULL
);
