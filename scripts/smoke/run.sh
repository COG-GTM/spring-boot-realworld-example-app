#!/usr/bin/env bash
# Boots the app with `./gradlew bootRun` (SQLite dev.db, Flyway migrations), runs the smoke
# check and compares status codes and response shapes with scripts/smoke/baseline.jsonl.
#
# Usage: scripts/smoke/run.sh [--update-baseline]
#   PORT (default 8080), BOOT_TIMEOUT seconds (default 300), BASE_URL to skip booting.
set -euo pipefail

cd "$(dirname "$0")/../.."
SMOKE_DIR=scripts/smoke
BASELINE=$SMOKE_DIR/baseline.jsonl
OUT_DIR=build/smoke
OUT=$OUT_DIR/actual.jsonl
PORT=${PORT:-8080}
BOOT_TIMEOUT=${BOOT_TIMEOUT:-300}
UPDATE_BASELINE=false
[ "${1:-}" = "--update-baseline" ] && UPDATE_BASELINE=true

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "$tool is required" >&2; exit 2; }
done
mkdir -p "$OUT_DIR"

APP_PID=
stop_app() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    pkill -TERM -P "$APP_PID" 2>/dev/null || true
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap stop_app EXIT

if [ -z "${BASE_URL:-}" ]; then
  BASE_URL=http://localhost:$PORT
  echo "Starting app with ./gradlew bootRun on port $PORT (log: $OUT_DIR/app.log)"
  ./gradlew --no-daemon bootRun --args="--server.port=$PORT" >"$OUT_DIR/app.log" 2>&1 &
  APP_PID=$!
  deadline=$((SECONDS + BOOT_TIMEOUT))
  until curl -s -o /dev/null "$BASE_URL/tags"; do
    if ! kill -0 "$APP_PID" 2>/dev/null || [ "$SECONDS" -ge "$deadline" ]; then
      echo "App failed to start; last log lines:" >&2
      tail -50 "$OUT_DIR/app.log" >&2
      exit 1
    fi
    sleep 2
  done
  echo "App is up after ${SECONDS}s"
fi

BASE_URL=$BASE_URL OUT=$OUT "$SMOKE_DIR/smoke.sh"

if $UPDATE_BASELINE; then
  cp "$OUT" "$BASELINE"
  echo "Baseline updated: $BASELINE"
  exit 0
fi

if diff -u "$BASELINE" "$OUT" >"$OUT_DIR/baseline.diff"; then
  echo "Responses match baseline ($BASELINE)"
else
  echo "Responses differ from baseline ($BASELINE):"
  cat "$OUT_DIR/baseline.diff"
  exit 1
fi
