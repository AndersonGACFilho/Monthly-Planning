# Changelog

Versions follow the `v<major>.<minor>.<patch>` tags. Since in-app updates, every green commit on `main` is released
with the commits since the previous release as notes (POR-17, see README "Releases"); this file keeps the
milestones.

## Unreleased

- **Export the database in one click**: *Exportar banco* at the top of the window (Ctrl+Shift+E) and a button in
  *Settings → Backups and data* download the whole database as `planejamento-mensal-<date>.db`, a consistent copy,
  also in the browser preview.
- **Import a database file**: *Settings → Backups and data → Import database (.db)* checks the file, keeps it with
  the backups as *Imported* and opens it to check; *Restore* makes it the live data after backing them up. A file
  that is not an intact database of the app is refused and nothing is kept.
- **The log no longer grows forever**: over 5 MB, `backend.log` moves to `backend.1.log` when the app starts, and
  only the three previous logs are kept.
- **Deleting incomes works again**: deleting an income or fixed expense failed with an error as soon as one of its
  months had been shown. One with a received or paid month is now ended from a chosen month: earlier months stay,
  later months not yet received or paid are removed. Each month row offers delete or end, without opening the list
  of rules.
- **Future expenses in the statement**: the block formerly called *future installments* lists, for each of the next
  three months, the member's installments and subscriptions and the fixed expenses paid outside the card, with the
  month total. The XLSX and CSV formats carry the same lines. Statements sent before show as outdated.
- **Subscription categories**: a category can be marked as subscription. A single card purchase classified in it
  becomes a subscription `*` and is projected into the next months; the overview and the statement summary show the
  subscriptions total, fixed expenses in those categories included. Existing categories start unmarked.
- **Pay a bill before it closes**: an open card bill can be marked as paid, after a confirmation; importing the
  closed bill later keeps it paid.
- **Debug mode**: *Settings → Diagnostics* turns on detailed logs and error notices with code, request number and
  address to copy; interface errors also reach the log. Every error now carries a request number found in
  `data/logs/backend.log`, unexpected errors no longer show a misleading "could not reach the local service", and a
  screen that fails to open offers to reload.
- **In-app updates**: the app looks for a new version when it opens and shows a notice with the release notes. On
  request it downloads the release, checks its SHA-256, and installs it when the app restarts, going back to the
  previous version if the new one does not open. `data/` is kept as it is. This is the last update installed by
  hand: replace the files once as described in `LEIA-ME.md`, and the next ones come from inside the app.
- **Find the app by searching the computer**: *Settings → System menu* adds the app to the Start Menu (Windows) or
  the app launcher (Linux) of that computer, on request only. It tells when the shortcut points at another drive
  letter or mount path and fixes it, and removing it deletes everything it wrote.
- **A release for every commit on main**: versions `v0.3.<n>` are computed from `VERSION` and the commit count, and
  each green commit is tagged and published with its commit list as notes.
- **Full statement layout** (TPL-07): the advanced template mode now edits the whole statement body as HTML. Parts
  of the statement are placed with placeholders (`{{header}}`, `{{warnings}}`, `{{block.summary}}`,
  `{{block.cards}}`, `{{block.fixedExpenses}}`, `{{block.variableExpenses}}`, `{{block.committed}}`,
  `{{block.payment}}`, `{{block.note}}`, `{{footer}}`) next to the message variables; the editor starts from the
  standard layout and lists every class and id of the statement with what it marks, so extra CSS can target them.
  The layout is sanitized like the header HTML. Every part and line of the standard statement now has a stable id or
  class.
- **Fine control of each export**: the Deliveries screen can change, for that export only, the header and footer
  texts (template variables allowed), which blocks are shown and in what order, and whether the date, installment and
  share columns appear. The template stays as it is; the preview, downloads and deliveries sent from there follow the
  changes. With a full layout, unticked blocks stay empty where their placeholder is.
- **Template guide**: the template editor copies a guide with the rules, the answer format, every class and id, the
  standard layout, the current template and a statement with sample data, to have a template written elsewhere (an
  LLM, for example); **Apply JSON** brings the answer back into the editor for review before saving.
- **Export feedback**: every download (statements, monthly spreadsheet, data export, spreadsheet model) ends with a
  notice naming the file, and its buttons rest for a second so a double click never exports twice.
- **Every item of a category**: a category in the overview opens all of its items (fixed expenses, variable expenses
  and card entries), each group with its subtotal and a shortcut to its screen, instead of only the card entries.
- **What was left of the month**: the overview shows the month's leftover with its share of the income, a
  spent-versus-left bar and the leftover of the last six months; a month in the chart opens it.
- **Current month** stands out in the period selector; any other month says how far it is and offers a button back
  to the current one.
- **Month on start**: when the app opens it asks which month to work on, with the current one highlighted (Enter
  opens it). It can be turned off in the dialog or in Settings.
- **Loading screen** with the app name, a progress bar and what the local service is doing (starting, opening the
  data, start backup and database check, a note when it takes longer than usual); on failure it says what to check
  and offers to try again.
- Night theme: buttons inside the backup band and primary buttons are readable again.
- **Check and restore backups**: each backup in Settings can be opened to check it across the app, with a yellow band
  and a way back to the live data; the backup file never changes and changes made to the copy are dropped. A backup
  can also be restored as the live data, after the current data is backed up. Backups opened from an older version
  are migrated in the copy.

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
