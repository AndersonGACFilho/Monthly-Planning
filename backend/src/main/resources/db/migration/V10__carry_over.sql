-- Amount a third-party member still owes at the end of a period, carried into the next one (RF-28)
CREATE TABLE carry_over (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id   INTEGER NOT NULL REFERENCES member (id),
    from_period TEXT    NOT NULL,
    to_period   TEXT    NOT NULL,
    amount      INTEGER NOT NULL CHECK (amount > 0),
    version     INTEGER NOT NULL DEFAULT 0,
    UNIQUE (member_id, from_period)
);
CREATE INDEX carry_over_to_period ON carry_over (to_period);
