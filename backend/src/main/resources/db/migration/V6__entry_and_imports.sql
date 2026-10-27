-- Bills, purchases and entries (SPEC section 10), import batches with their preview lines (Q16) and audit (RNF-05).
CREATE TABLE card_bill (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id        INTEGER NOT NULL REFERENCES card (id),
    billing_period TEXT    NOT NULL,
    closing_date   TEXT,
    due_date       TEXT,
    file_total     INTEGER,
    status         TEXT    NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'PAID')),
    partial        INTEGER NOT NULL DEFAULT 0,
    version        INTEGER NOT NULL DEFAULT 0,
    UNIQUE (card_id, billing_period)
);

CREATE TABLE purchase (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id                 INTEGER NOT NULL REFERENCES card (id),
    card_last_digits        TEXT,
    purchase_date           TEXT    NOT NULL,
    original_description    TEXT    NOT NULL,
    normalized_description  TEXT    NOT NULL,
    installment_amount      INTEGER NOT NULL,
    installment_count       INTEGER NOT NULL DEFAULT 1,
    first_known_installment INTEGER NOT NULL DEFAULT 1,
    type                    TEXT    NOT NULL CHECK (type IN ('SINGLE', 'INSTALLMENT', 'SUBSCRIPTION')),
    kind                    TEXT    NOT NULL CHECK (kind IN ('PURCHASE', 'CREDIT', 'REFUND', 'IOF', 'FEE')),
    bank_category           TEXT,
    purchase_key            TEXT    NOT NULL,
    category_id             INTEGER REFERENCES category (id),
    note                    TEXT,
    review_status           TEXT    NOT NULL CHECK (review_status IN ('PENDING_REVIEW', 'REVIEWED')),
    manually_edited         INTEGER NOT NULL DEFAULT 0,
    refund_of_purchase_id   INTEGER REFERENCES purchase (id),
    refund_status           TEXT    NOT NULL DEFAULT 'NONE' CHECK (refund_status IN ('NONE', 'PARTIAL', 'FULL')),
    refunded_amount         INTEGER NOT NULL DEFAULT 0,
    unknown_final           INTEGER NOT NULL DEFAULT 0,
    version                 INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX purchase_card ON purchase (card_id);
CREATE INDEX purchase_normalized ON purchase (normalized_description);
CREATE INDEX purchase_review ON purchase (review_status);

CREATE TABLE purchase_split (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id INTEGER NOT NULL REFERENCES purchase (id) ON DELETE CASCADE,
    member_id   INTEGER NOT NULL REFERENCES member (id),
    percentage  NUMERIC,
    amount      INTEGER
);
CREATE INDEX purchase_split_member ON purchase_split (member_id);

CREATE TABLE bill_entry (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id        INTEGER NOT NULL REFERENCES purchase (id),
    card_bill_id       INTEGER NOT NULL REFERENCES card_bill (id),
    installment_number INTEGER,
    entry_date         TEXT    NOT NULL,
    amount             INTEGER NOT NULL,
    status             TEXT    NOT NULL CHECK (status IN ('PROJECTED', 'CONFIRMED', 'UNCONFIRMED')),
    installment_key    TEXT    NOT NULL,
    related_entry_id   INTEGER REFERENCES bill_entry (id),
    import_batch_id    INTEGER,
    version            INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX bill_entry_bill ON bill_entry (card_bill_id);
CREATE INDEX bill_entry_purchase ON bill_entry (purchase_id);
CREATE INDEX bill_entry_key ON bill_entry (installment_key);

CREATE TABLE batch_change (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id             INTEGER NOT NULL,
    change_type          TEXT    NOT NULL,
    target_id            INTEGER NOT NULL,
    previous_status      TEXT,
    previous_amount      INTEGER,
    previous_date        TEXT,
    previous_bill_id     INTEGER,
    previous_batch_id    INTEGER,
    previous_bill_status TEXT,
    previous_partial     INTEGER,
    previous_closing     TEXT,
    previous_due         TEXT,
    removed_purchase_id  INTEGER,
    removed_installment  INTEGER,
    removed_key          TEXT
);
CREATE INDEX batch_change_batch ON batch_change (batch_id);

CREATE TABLE import_batch (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id               INTEGER NOT NULL REFERENCES card (id),
    billing_period        TEXT    NOT NULL,
    format                TEXT    NOT NULL CHECK (format IN ('PDF', 'XLSX')),
    file_name             TEXT    NOT NULL,
    file_hash             TEXT    NOT NULL,
    imported_at           TEXT    NOT NULL,
    status                TEXT    NOT NULL CHECK (status IN ('PREVIEW', 'CONFIRMED', 'CANCELLED', 'UNDONE')),
    open_bill             INTEGER NOT NULL,
    due_date              TEXT,
    closing_date          TEXT,
    next_closing_forecast TEXT,
    card_last_digits      TEXT,
    file_total            INTEGER,
    entries_total         INTEGER,
    computed_total        INTEGER NOT NULL,
    totals_match          INTEGER NOT NULL,
    totals_acknowledged   INTEGER NOT NULL DEFAULT 0,
    warnings              TEXT,
    new_count             INTEGER NOT NULL DEFAULT 0,
    duplicate_count       INTEGER NOT NULL DEFAULT 0,
    matched_count         INTEGER NOT NULL DEFAULT 0,
    divergent_count       INTEGER NOT NULL DEFAULT 0,
    version               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX import_batch_card ON import_batch (card_id, status);

CREATE TABLE import_line (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id            INTEGER NOT NULL REFERENCES import_batch (id) ON DELETE CASCADE,
    sequence            INTEGER NOT NULL,
    kind                TEXT    NOT NULL,
    purchase_date       TEXT    NOT NULL,
    description         TEXT    NOT NULL,
    installment_number  INTEGER,
    installment_count   INTEGER,
    amount              INTEGER NOT NULL,
    holder_name         TEXT,
    card_last_digits    TEXT,
    bank_category       TEXT,
    note                TEXT,
    related_description TEXT,
    future_check        INTEGER NOT NULL DEFAULT 0,
    occurrence          INTEGER NOT NULL DEFAULT 1,
    purchase_key        TEXT    NOT NULL,
    installment_key     TEXT    NOT NULL,
    status              TEXT    NOT NULL,
    matched_entry_id    INTEGER,
    decision            TEXT    NOT NULL,
    member_id           INTEGER,
    category_id         INTEGER,
    alias_name          TEXT,
    unknown_final       INTEGER NOT NULL DEFAULT 0,
    created_entry_id    INTEGER,
    version             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX import_line_batch ON import_line (batch_id);

CREATE TABLE audit_entry (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT    NOT NULL,
    entity_id   INTEGER NOT NULL,
    changed_at  TEXT    NOT NULL,
    before_json TEXT    NOT NULL,
    after_json  TEXT    NOT NULL
);
CREATE INDEX audit_entry_entity ON audit_entry (entity_type, entity_id);
