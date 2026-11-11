-- A category can be marked as subscription: single card purchases in it become subscriptions (IMP-65).
-- Existing categories start unmarked; the user marks them on the categories screen.
ALTER TABLE category ADD COLUMN subscription INTEGER NOT NULL DEFAULT 0;
