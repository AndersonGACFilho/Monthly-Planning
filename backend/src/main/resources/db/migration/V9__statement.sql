CREATE TABLE template (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT    NOT NULL,
    default_template INTEGER NOT NULL DEFAULT 0,
    settings_json    TEXT    NOT NULL,
    current_version  INTEGER NOT NULL DEFAULT 1,
    updated_at       TEXT    NOT NULL,
    version          INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE template_version (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id   INTEGER NOT NULL REFERENCES template (id) ON DELETE CASCADE,
    number        INTEGER NOT NULL,
    settings_json TEXT    NOT NULL,
    created_at    TEXT    NOT NULL,
    UNIQUE (template_id, number)
);

CREATE TABLE member_template (
    member_id   INTEGER PRIMARY KEY REFERENCES member (id) ON DELETE CASCADE,
    template_id INTEGER NOT NULL REFERENCES template (id) ON DELETE CASCADE
);

CREATE TABLE statement_delivery (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id        INTEGER NOT NULL REFERENCES member (id),
    billing_period   TEXT    NOT NULL,
    channel          TEXT    NOT NULL CHECK (channel IN ('EMAIL', 'WHATSAPP', 'CLIPBOARD', 'FOLDER')),
    status           TEXT    NOT NULL CHECK (status IN ('SENT', 'AWAITING_USER', 'FAILED')),
    created_at       TEXT    NOT NULL,
    template_id      INTEGER NOT NULL,
    template_version INTEGER NOT NULL,
    language         TEXT    NOT NULL,
    fingerprint      TEXT    NOT NULL,
    show_categories  INTEGER NOT NULL,
    use_alias        INTEGER NOT NULL,
    include_future   INTEGER NOT NULL,
    detail           TEXT
);
CREATE INDEX statement_delivery_period ON statement_delivery (billing_period, member_id);
