#!/bin/sh
# Planejamento Mensal - Linux fallback launcher (POR-09).
# Use it when the AppImage does not open: no FUSE on the system, or the drive is mounted with "noexec"
# (common for exFAT SSDs). It copies the program and the Java runtime to a temporary folder, runs them from
# there and removes the copy when the app closes. Your data stays in the data/ folder next to this script.
set -eu

HOME_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TMP=$(mktemp -d "${TMPDIR:-/tmp}/planejamento-mensal.XXXXXX")
cleanup() { rm -rf "$TMP"; }
trap cleanup EXIT INT TERM
# Exit code of the app after it installed an update: copy the new files and start again (POR-16)
RESTART=75

# Copies the program and the JRE from the app folder; sets RUN to what must be started
prepare() {
  rm -rf "$TMP/jre" "$TMP/native" "$TMP/app.AppImage" "$TMP/squashfs-root" "$TMP/PlanejamentoMensal"
  cp -R "$HOME_DIR/backend/runtime-linux" "$TMP/jre"
  # Native libraries (SQLite) are unpacked here too: a noexec drive cannot load them from data/tmp
  mkdir -p "$TMP/native"
  chmod -R u+x "$TMP/jre/bin" "$TMP/jre/lib" 2>/dev/null || true

  if [ -f "$HOME_DIR/PlanejamentoMensal.AppImage" ]; then
    cp "$HOME_DIR/PlanejamentoMensal.AppImage" "$TMP/app.AppImage"
    chmod u+x "$TMP/app.AppImage"
    (cd "$TMP" && ./app.AppImage --appimage-extract >/dev/null)
    RUN="$TMP/squashfs-root/AppRun"
  else
    cp "$HOME_DIR/PlanejamentoMensal" "$TMP/PlanejamentoMensal"
    chmod u+x "$TMP/PlanejamentoMensal"
    RUN="$TMP/PlanejamentoMensal"
  fi
}

prepare
if [ -n "${PM_START_DRY_RUN:-}" ]; then
  # Used by the packaging smoke test: everything is prepared, nothing is started
  echo "would run: PM_APP_HOME=$HOME_DIR PM_JAVA_HOME=$TMP/jre PM_NATIVE_DIR=$TMP/native $RUN"
  "$TMP/jre/bin/java" -version 2>&1 | head -1
  test -x "$RUN"
  exit 0
fi
while :; do
  STATUS=0
  PM_APP_HOME="$HOME_DIR" PM_JAVA_HOME="$TMP/jre" PM_NATIVE_DIR="$TMP/native" PM_LAUNCHER=start-linux.sh \
    "$RUN" "$@" || STATUS=$?
  [ "$STATUS" -eq "$RESTART" ] || exit "$STATUS"
  prepare
done
