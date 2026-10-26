CREATE TABLE member (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    code               TEXT    NOT NULL UNIQUE,
    name               TEXT    NOT NULL,
    kind               TEXT    NOT NULL CHECK (kind IN ('MAIN', 'THIRD_PARTY')),
    color              TEXT    NOT NULL,
    active             INTEGER NOT NULL DEFAULT 1,
    email              TEXT,
    whatsapp_phone     TEXT,
    pix_key            TEXT,
    preferred_language TEXT    NOT NULL DEFAULT 'pt-BR',
    version            INTEGER NOT NULL DEFAULT 0
);
