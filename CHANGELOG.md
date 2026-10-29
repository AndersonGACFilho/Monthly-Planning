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
- **A release for every commit on main**: versions `v0.2.<n>` are computed from `VERSION` and the commit count, and
  each green commit is tagged and published with its commit list as notes.

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
