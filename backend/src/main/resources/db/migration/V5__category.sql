CREATE TABLE category (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    parent_id       INTEGER REFERENCES category (id),
    color           TEXT    NOT NULL,
    icon            TEXT    NOT NULL,
    system_category INTEGER NOT NULL DEFAULT 0,
    version         INTEGER NOT NULL DEFAULT 0
);
