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

## Real bills

Real Itaú bills and spreadsheets never enter this repository. Tests use the masked copies in `backend/src/test/resources/fixtures/itau` (see its README). The `.gitignore` rejects any other `.pdf`, `.xlsx` or `.csv`.
