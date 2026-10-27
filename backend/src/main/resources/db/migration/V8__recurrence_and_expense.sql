CREATE TABLE recurrence_rule (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    kind            TEXT    NOT NULL CHECK (kind IN ('INCOME', 'FIXED_EXPENSE')),
    description     TEXT    NOT NULL,
    member_id       INTEGER NOT NULL REFERENCES member (id),
    category_id     INTEGER REFERENCES category (id),
    income_type     TEXT,
    default_amount  INTEGER NOT NULL,
    recurrence      TEXT    NOT NULL CHECK (recurrence IN ('MONTHLY', 'ONCE', 'EVERY_N_MONTHS')),
    interval_months INTEGER NOT NULL DEFAULT 1,
    day             INTEGER NOT NULL CHECK (day BETWEEN 1 AND 31),
    valid_from      TEXT    NOT NULL,
    valid_until     TEXT,
    payment_method  TEXT,
    active          INTEGER NOT NULL DEFAULT 1,
    version         INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE occurrence (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    recurrence_rule_id INTEGER NOT NULL REFERENCES recurrence_rule (id) ON DELETE CASCADE,
    billing_period     TEXT    NOT NULL,
    amount             INTEGER NOT NULL,
    amount_overridden  INTEGER NOT NULL DEFAULT 0,
    status             TEXT    NOT NULL CHECK (status IN ('EXPECTED', 'DONE')),
    effective_date     TEXT,
    linked_entry_id    INTEGER REFERENCES bill_entry (id) ON DELETE SET NULL,
    link_candidates    TEXT,
    version            INTEGER NOT NULL DEFAULT 0,
    UNIQUE (recurrence_rule_id, billing_period)
);
CREATE INDEX occurrence_period ON occurrence (billing_period);

CREATE TABLE variable_expense (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    expense_date   TEXT    NOT NULL,
    billing_period TEXT    NOT NULL,
    description    TEXT    NOT NULL,
    member_id      INTEGER NOT NULL REFERENCES member (id),
    category_id    INTEGER REFERENCES category (id),
    amount         INTEGER NOT NULL,
    payment_method TEXT    NOT NULL,
    version        INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX variable_expense_period ON variable_expense (billing_period);
