#!/bin/bash
# Load test: 100 users, 10/sec spawn rate, 3 minutes

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$SCRIPT_DIR/reports"

echo "Starting Load Test..."
echo "Users: 100 | Spawn Rate: 10/sec | Duration: 3m"

"$SCRIPT_DIR/venv/bin/locust" -f "$SCRIPT_DIR/locustfile.py" \
    --headless \
    --users 100 \
    --spawn-rate 10 \
    --run-time 3m \
    --host http://localhost:8900 \
    --html "$SCRIPT_DIR/reports/load-test-$(date +%Y%m%d-%H%M%S).html" \
    --csv "$SCRIPT_DIR/reports/load-test-$(date +%Y%m%d-%H%M%S)"

echo "Load test completed! Check reports/ directory"