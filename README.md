# Planejamento Mensal

Portable desktop app (Windows and Linux) for monthly household finances: incomes, fixed and variable expenses and Itaú credit card bills, split by member. Requirements live in [docs/SPEC.md](docs/SPEC.md); the work plan and its progress live in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

## Layout

| Folder | Content |
| --- | --- |
| `backend/` | Spring Boot 4 API (Java 25), Maven wrapper |
| `frontend/` | React 19 + Vite + TypeScript interface |
| `src-tauri/` | Tauri 2 desktop shell that starts the backend sidecar |
| `scripts/` | Build and packaging scripts |
| `docs/` | Specification and implementation plan |

## Prerequisites

- JDK 25 (`JAVA_HOME` must point to it; the build refuses other versions)
- Node.js 22 or newer and npm
- Rust (stable) for the Tauri shell, plus WebKitGTK 4.1 on Linux

## Backend

```bash
cd backend && ./mvnw verify
```

Tests, Javadoc (DOC-04) and coverage gates (TST-04) run in `verify`. Flags for deliberate changes:
`-Dopenapi.update=true` (API contract), `-Dsnapshot.update=true` (statement HTML snapshots),
`-Dworkbook.model.update=true` (bundled spreadsheet model, rebuilt from the masked fixture).

## Frontend

```bash
cd frontend && npm ci && npm run typecheck && npm run lint && npm test
```

The API client is generated from `frontend/openapi/openapi.json` before build, typecheck and tests.

## End-to-end tests

Build the backend jar first, then:

```bash
cd frontend && npx playwright install chromium webkit && npm run e2e
```

`E2E_ENGINES=chromium` skips WebKit on hosts without its system libraries. The run starts the jar on port 18090
with a fresh data folder seeded from the masked bills.

## Portable package

```bash
python scripts/build_runtime.py backend/target/app.jar build/runtime
frontend/node_modules/.bin/tauri build --bundles appimage
python scripts/assemble_portable.py --os linux --shell src-tauri/target/release/PlanejamentoMensal \
  --jar backend/target/app.jar --runtime build/runtime --out dist/PlanejamentoMensal \
  --appimage src-tauri/target/release/bundle/appimage/PlanejamentoMensal_0.1.0_amd64.AppImage
python scripts/portable_smoke.py dist/PlanejamentoMensal --os linux
```

The Windows half is built the same way on Windows (`--os windows`, `PlanejamentoMensal.exe`); CI assembles both
into one zip. Users find start, backup, ejecting and update instructions in `LEIA-ME.md` inside the folder.

The folder writes nothing outside itself (POR-03) with two exceptions: the temporary copy of `start-linux.sh`
(POR-09), and the system menu entry the user may add in Settings (POR-18, `src-tauri/src/menu_shortcut/`): a Start
Menu `.lnk` on Windows, a `.desktop` file and icon in `~/.local/share/applications/` on Linux. It is never created
at start, so `portable_smoke.py` still finds nothing outside the folder.

## Releases

Every commit pushed to `main` is a release once all CI jobs pass on it (POR-17). Its version is
`<VERSION>.<patch>`: `VERSION` holds the major and minor, and the patch counts the commits on `main` since `VERSION`
last changed, so each commit has exactly one version and a re-run neither changes nor duplicates it. To start a
new minor or major, change `VERSION` (for example to `0.3`); that commit is `v0.3.0`.

`scripts/version.py compute` gives the version and `scripts/version.py apply <version>` writes it into
`tauri.conf.json`, `Cargo.toml`, `package.json`, `package-lock.json` and `pom.xml` in the CI working tree; the
repository keeps `0.0.0` there and nothing is committed back. Branches and pull requests build
`<version>-dev.<commit>`, a pre-release the app never offers.

CI builds the portable zip on Windows and Linux, tags the commit `v<version>` and publishes the zip with a
`PlanejamentoMensal-v<version>.zip.sha256` file. The release notes are the subjects of the commits since the
previous release (`scripts/release_notes.py`), and the app shows them in its update notice, so write commit
subjects for the people who use the app.

Installed apps find it by themselves (POR-16): at start the shell reads the
[releases API](https://api.github.com/repos/AndersonGACFilho/Monthly-Planning/releases), offers the newest final
(not draft, not pre-release) version above its own, downloads the zip into `update/` in the app folder, checks the
SHA-256 and swaps the files at the next start, rolling back if the new backend does not start. Drafts and
pre-releases are never offered, so a release can be tested by marking it as a pre-release first. `PM_UPDATE_URL`
points the shell at another release list (local tests). Development builds check but never install.

## Real bills

Real Itaú bills and spreadsheets never enter this repository. Tests use the masked copies in `backend/src/test/resources/fixtures/itau` (see its README). The `.gitignore` rejects any other `.pdf`, `.xlsx` or `.csv`.
