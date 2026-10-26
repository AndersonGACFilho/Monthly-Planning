CREATE TABLE app_setting (
    setting_key   TEXT    PRIMARY KEY,
    setting_value TEXT    NOT NULL,
    version       INTEGER NOT NULL DEFAULT 0
);
