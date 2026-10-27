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

## Real bills

Real Itaú bills and spreadsheets never enter this repository. Tests use the masked copies in `backend/src/test/resources/fixtures/itau` (see its README). The `.gitignore` rejects any other `.pdf`, `.xlsx` or `.csv`.
