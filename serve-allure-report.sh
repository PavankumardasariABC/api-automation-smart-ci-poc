#!/usr/bin/env bash
# Serve Allure report so it opens correctly in Safari/Chrome (no "Loading..." issue).
# Open: http://localhost:8765/Allure_Offline_Single_Safe.html or http://localhost:8765/

set -e
REPORT_DIR="reports/current"
PORT="${1:-8765}"

if [ ! -d "$REPORT_DIR" ]; then
  echo "❌ No report found at $REPORT_DIR"
  echo "   Run first: ./gradlew test -Dgroups=Billing"
  exit 1
fi

echo "📂 Serving Allure report from $REPORT_DIR"
echo "   Open in browser: http://localhost:$PORT/Allure_Offline_Single_Safe.html"
echo "   or:              http://localhost:$PORT/"
echo "   Press Ctrl+C to stop the server."
echo ""

cd "$REPORT_DIR"
python3 -m http.server "$PORT"
