#!/usr/bin/env bash
# End-to-end backend for Playwright: the built jar on port 18090, token "e2e", a fresh data folder every run.
# Runs in the foreground so Playwright can stop it. Build first: (cd backend && ./mvnw -q package -DskipTests).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOME_DIR="${E2E_HOME:-${TMPDIR:-/tmp}/planejamento-mensal-e2e}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
rm -rf "$HOME_DIR" && mkdir -p "$HOME_DIR"
cp "$ROOT/backend/target/app.jar" "$HOME_DIR/app.jar"
export APP_SESSION_TOKEN=e2e
exec "$JAVA" -jar "$HOME_DIR/app.jar" --app.home="$HOME_DIR" --server.port=18090 \
  --app.web.allowed-origins=http://localhost:5174,http://127.0.0.1:5174
