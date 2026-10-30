# Changelog

Versions follow the `v<major>.<minor>.<patch>` tags. Since in-app updates, every green commit on `main` is released
with the commits since the previous release as notes (POR-17, see README "Releases"); this file keeps the
milestones.

## Unreleased

- **In-app updates**: the app looks for a new version when it opens and shows a notice with the release notes. On
  request it downloads the release, checks its SHA-256, and installs it when the app restarts, going back to the
  previous version if the new one does not open. `data/` is kept as it is. This is the last update installed by
  hand: replace the files once as described in `LEIA-ME.md`, and the next ones come from inside the app.
- **Find the app by searching the computer**: *Settings → System menu* adds the app to the Start Menu (Windows) or
  the app launcher (Linux) of that computer, on request only. It tells when the shortcut points at another drive
  letter or mount path and fixes it, and removing it deletes everything it wrote.
- **A release for every commit on main**: versions `v0.3.<n>` are computed from `VERSION` and the commit count, and
  each green commit is tagged and published with its commit list as notes.

## v0.2.0 — 2026-10-30

Carry-over of unpaid balances, splits by amount and a faster way through card entries.

- **Carry over an unpaid balance** (RF-28): what a member still owes after the incomes they paid in a month can move
  to the next month as a previous balance. The entries stay in their own billing period, so each month's total is
  unchanged; the source month shows what was carried out. Carrying again updates the amount and a carry-over can be
  undone. Statements, their spreadsheet and CSV, and the monthly workbook show the previous balance and the carried
  amount. Existing data is migrated when the app starts.
- **Splits by amount or by percentage**: the split editor toggles between the two, keeps each member's part to the
  cent, divides the whole evenly and says how much is missing or over. A split saved by amount is now read back as
  amounts instead of 100% for one member. The entry details panel can edit the split, and inline edits reject an
  incomplete one.
- **Manual entries** choose their bill, default the date to the local day and warn when there is no card. A manual
  purchase that no imported bill has seen can be edited or deleted, and editing keeps its note.
- **Overview**: a category in the table or the chart opens the Entries tab filtered by that category, period and member;
  the chart height follows the number of categories.
- **Category icons** are picked from a searchable grid of every icon, with suggestions first.
- Success notices close after 4 seconds and errors after 8; Escape closes only the topmost dialog.
- Fixes: category chart slices showed as "not classified" while categories loaded, and split amounts overflowed the
  narrow details panel.

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
