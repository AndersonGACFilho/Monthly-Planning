#!/usr/bin/env bash
# Development helper: (re)starts the backend from a copy of the jar on port 18080 with token "dev" and a
# fresh data folder, then loads sample data from the masked fixtures. The copy keeps rebuilds from
# replacing the jar under the running process.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOME_DIR="${DEV_HOME:-${TMPDIR:-/tmp}/planejamento-mensal-dev}"
PORT=18080
U="http://127.0.0.1:$PORT"
H="X-Session-Token: dev"
JAVA="${JAVA_HOME:?set JAVA_HOME to JDK 25}/bin/java"

curl -s -m 5 -X POST -H "$H" "$U/actuator/shutdown" >/dev/null 2>&1 || true
for _ in $(seq 1 20); do curl -sf "$U/actuator/health" >/dev/null 2>&1 || break; sleep 1; done
rm -rf "$HOME_DIR" && mkdir -p "$HOME_DIR"
cp "$ROOT/backend/target/app.jar" "$HOME_DIR/app.jar"
APP_SESSION_TOKEN=dev nohup "$JAVA" -jar "$HOME_DIR/app.jar" --app.home="$HOME_DIR" --server.port=$PORT >"$HOME_DIR/backend.out" 2>&1 &
for _ in $(seq 1 60); do curl -sf "$U/actuator/health" >/dev/null 2>&1 && break; sleep 1; done

json() { curl -s -H "$H" -H "Content-Type: application/json" "$@"; }
id() { python3 -I -c "import json,sys; print(json.load(sys.stdin)['id'])"; }
MAIN=$(json -d '{"code":"AB","name":"Casa","kind":"MAIN","color":"#2563EB","preferredLanguage":"pt-BR"}' "$U/api/members" | id)
json -d '{"code":"M","name":"Mãe","kind":"THIRD_PARTY","color":"#DB2777","preferredLanguage":"pt-BR","whatsappPhone":"+5551999990000"}' "$U/api/members" >/dev/null
json -X POST -d '{"language":"pt-BR"}' "$U/api/categories/defaults" >/dev/null
CARD=$(json -d "{\"nickname\":\"Itaú Platinum\",\"bank\":\"ITAU\",\"lastDigits\":[\"4102\",\"4090\",\"9103\"],\"holderName\":\"MARIANA S F SILVA\",\"defaultMemberId\":$MAIN,\"closingDay\":2,\"dueDay\":9}" "$U/api/cards" | id)
FIX="$ROOT/backend/src/test/resources/fixtures/itau"
B1=$(curl -s -H "$H" -F "file=@$FIX/itau-closed-bill-2026-10.pdf" -F cardId=$CARD -F billingPeriod=2026-10 "$U/api/imports" | id)
curl -s -H "$H" -X POST "$U/api/imports/$B1/confirmation" >/dev/null
B2=$(curl -s -H "$H" -F "file=@$FIX/itau-open-bill-2026-11.xlsx" -F cardId=$CARD -F billingPeriod=2026-11 "$U/api/imports" | id)
curl -s -H "$H" -X POST "$U/api/imports/$B2/confirmation" >/dev/null
echo "Dev backend on $U (token dev), data in $HOME_DIR"
