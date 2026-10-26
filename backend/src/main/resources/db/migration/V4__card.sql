CREATE TABLE card (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    nickname          TEXT    NOT NULL,
    bank              TEXT    NOT NULL,
    holder_name       TEXT    NOT NULL,
    primary_card_id   INTEGER REFERENCES card (id),
    default_member_id INTEGER NOT NULL REFERENCES member (id),
    active            INTEGER NOT NULL DEFAULT 1,
    version           INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE card_final (
    card_id     INTEGER NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    last_digits TEXT    NOT NULL,
    PRIMARY KEY (card_id, last_digits)
);

CREATE TABLE card_cycle (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id     INTEGER NOT NULL REFERENCES card (id) ON DELETE CASCADE,
    closing_day INTEGER NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day     INTEGER NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    valid_from  TEXT    NOT NULL,
    UNIQUE (card_id, valid_from)
);
