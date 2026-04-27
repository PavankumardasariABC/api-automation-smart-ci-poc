#!/usr/bin/env bash
# Serves Allure over HTTP so index.html can load /data/*.json (avoids file:// issues).
# For file:// use reports/current/index.html after ./gradlew generateAllureReport (embeds JSON in the page).
set -euo pipefail
MODULE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# Override: ALLURE_REPORT_DIR=/path/to/report ./scripts/serve-allure-report.sh
if [[ -n "${ALLURE_REPORT_DIR:-}" ]]; then
  REPORT_DIR="$ALLURE_REPORT_DIR"
elif [[ -d "${MODULE_DIR}/reports/current" ]] && [[ -f "${MODULE_DIR}/reports/current/index.html" ]]; then
  REPORT_DIR="${MODULE_DIR}/reports/current"
elif [[ -d "${MODULE_DIR}/allure-report" ]] && [[ -f "${MODULE_DIR}/allure-report/index.html" ]]; then
  REPORT_DIR="${MODULE_DIR}/allure-report"
else
  REPORT_DIR="${MODULE_DIR}/reports/current"
fi

_port_in_use() {
  local p="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$p" >/dev/null 2>&1
    return $?
  fi
  return 1
}

_pick_free_port() {
  local p
  for p in 8765 8766 8767 8768 8769 8770 8777 8780 8800 8888 9000; do
    if ! _port_in_use "$p"; then
      echo "$p"
      return 0
    fi
  done
  return 1
}

if [[ ! -d "$REPORT_DIR" ]] || [[ ! -f "$REPORT_DIR/index.html" ]]; then
  echo "No report at $REPORT_DIR"
  echo "Run: cd \"$MODULE_DIR\" && ./gradlew test && ./gradlew generateAllureReport"
  exit 1
fi

if [[ -n "${1:-}" ]]; then
  PORT="$1"
  if _port_in_use "$PORT"; then
    echo "Port $PORT is in use."
    exit 1
  fi
else
  if ! PORT="$(_pick_free_port)"; then
    echo "No free port found. Pass: $0 9292"
    exit 1
  fi
  if [[ "$PORT" != "8765" ]]; then
    echo "Note: 8765 busy — using $PORT"
    echo ""
  fi
fi

cd "$REPORT_DIR"
echo ""
echo "  http://127.0.0.1:${PORT}/index.html"
echo "  http://127.0.0.1:${PORT}/Allure_Offline_Single_Safe.html"
echo ""
if command -v python3 >/dev/null 2>&1; then
  exec python3 -m http.server "$PORT"
fi
echo "Install python3 for http.server"
exit 1
