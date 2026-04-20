#!/usr/bin/env bash
# Serves Allure so Safari/Chrome can load index.html (avoids file:// fetch restrictions).
set -euo pipefail
MODULE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPORT_DIR="${MODULE_DIR}/reports/current"

# True if something is listening on TCP port $1 (macOS / Linux)
_port_in_use() {
  local p="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  # Fallback: try to connect (busy if connect succeeds)
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
    echo "Port $PORT is already in use."
    echo "  See process: lsof -nP -iTCP:$PORT -sTCP:LISTEN"
    echo "  Or pick another: $0 8766"
    exit 1
  fi
else
  if ! PORT="$(_pick_free_port)"; then
    echo "Could not find a free port (tried 8765–9000). Close a server or pass a port: $0 9292"
    exit 1
  fi
  if [[ "$PORT" != "8765" ]]; then
    echo "Note: 8765 was busy — using port $PORT instead."
    echo ""
  fi
fi

cd "$REPORT_DIR"
echo ""
echo "Allure report — open ONE of these in your browser:"
echo "  http://127.0.0.1:${PORT}/index.html"
echo "  http://127.0.0.1:${PORT}/Allure_Offline_Single_Safe.html"
echo ""
echo "Press Ctrl+C to stop the server."
echo ""

if command -v python3 >/dev/null 2>&1; then
  exec python3 -m http.server "$PORT"
fi
if command -v python >/dev/null 2>&1; then
  exec python -m SimpleHTTPServer "$PORT" 2>/dev/null || exec python -m http.server "$PORT"
fi
echo "Install Python 3 or run: npx --yes serve -l $PORT ."
exit 1
