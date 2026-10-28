# Changelog

Versions follow the `v<major>.<minor>.<patch>` tags. Pushing a tag builds the portable zip in CI and publishes it as
a GitHub release with the notes of that version below (IMP-47).

## Unreleased

- **In-app updates**: the app looks for a new version when it opens and shows a notice with the release notes. On
  request it downloads the release, checks its SHA-256, and installs it when the app restarts, going back to the
  previous version if the new one does not open. `data/` is kept as it is. This is the last update installed by
  hand: replace the files once as described in `LEIA-ME.md`, and the next ones come from inside the app.

## v0.1.0 — 2026-10-28

First release of the portable app for Windows and Linux.

- **Overview** of the billing period: income, expense, balance, alerts, totals by category, member and card,
  committed installments, who owes whom and pending items.
- **History** of past months by month, quarter, semester or year, filtered by member and category, with charts.
- **Incomes and fixed expenses** with recurrence, **variable expenses** and **card bills** with splits between members.
- **Itaú import** of open (XLSX) and closed (PDF) bills: preview, deduplication, installments, refunds and a review
  queue with aliases, categories and members.
- **Member statements** as PDF, image or e-mail with editable templates, and the **monthly spreadsheet** export.
- **Registries** of members, cards and categories; **settings** for language (Portuguese or English), day and night
  themes, alerts, e-mail, spreadsheet and backups.
- **Portable**: one folder for Windows and Linux, data kept in `data/` next to the app, automatic backups, works from
  an exFAT drive (`start-linux.sh` when the drive does not allow running programs).
