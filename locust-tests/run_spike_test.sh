#!/bin/bash
# Spike test: Rapid increase to 1000 users

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$SCRIPT_DIR/reports"

echo "Starting Spike Test..."
echo "Users: 1000 | Spawn Rate: 100/sec | Duration: 1m"

"$SCRIPT_DIR/venv/bin/locust" -f "$SCRIPT_DIR/locustfile.py" \
    --headless \
    --users 1000 \
    --spawn-rate 100 \
    --run-time 1m \
    --host http://localhost:8900 \
    --html "$SCRIPT_DIR/reports/spike-test-$(date +%Y%m%d-%H%M%S).html" \
    --csv "$SCRIPT_DIR/reports/spike-test-$(date +%Y%m%d-%H%M%S)"

echo "Spike test completed! Check reports/ directory"