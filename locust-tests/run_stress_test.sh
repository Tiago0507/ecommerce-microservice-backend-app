#!/bin/bash
# Stress test: 500 users, 50/sec spawn rate, 2 minutes

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$SCRIPT_DIR/reports"

echo "Starting Stress Test..."
echo "Users: 500 | Spawn Rate: 50/sec | Duration: 2m"

"$SCRIPT_DIR/venv/bin/locust" -f "$SCRIPT_DIR/locustfile.py" \
    --headless \
    --users 500 \
    --spawn-rate 50 \
    --run-time 2m \
    --host http://localhost:8900 \
    --html "$SCRIPT_DIR/reports/stress-test-$(date +%Y%m%d-%H%M%S).html" \
    --csv "$SCRIPT_DIR/reports/stress-test-$(date +%Y%m%d-%H%M%S)"

echo "Stress test completed! Check reports/ directory"