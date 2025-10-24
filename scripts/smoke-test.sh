#!/usr/bin/env bash
set -euo pipefail

echo "[smoke] Checking Docker and Compose versions..."
docker version
docker compose version

wait_for_log() {
  local name="$1" pattern="$2" timeout="${3:-120}"
  echo "[smoke] Waiting for '$name' to log pattern: $pattern (timeout ${timeout}s)"
  SECS=0
  while (( SECS < timeout )); do
    if docker logs "$name" 2>&1 | grep -qE "$pattern"; then
      echo "[smoke] Pattern found in $name logs"
      return 0
    fi
    sleep 3
    SECS=$((SECS+3))
  done
  echo "[smoke] ERROR: Pattern not found in $name logs within ${timeout}s" >&2
  docker logs "$name" || true
  exit 1
}

echo "[smoke] docker compose ps (core)"
docker compose -f core.yml ps

echo "[smoke] docker compose ps (services)"
docker compose -f compose.yml ps

# Validate API Gateway started
API_GATEWAY_NAME=$(docker ps --format '{{.Names}}' | grep api-gateway-container | head -n1 || true)
if [[ -z "$API_GATEWAY_NAME" ]]; then
  echo "[smoke] ERROR: api-gateway container not found" >&2
  docker compose -f compose.yml ps || true
  exit 1
fi

wait_for_log "$API_GATEWAY_NAME" 'Started ApiGatewayApplication|Netty started on port 8080' 180

echo "[smoke] SUCCESS: Smoke checks passed"
