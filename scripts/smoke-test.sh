#!/bin/bash
set -euo pipefail

SERVICES=(
  "http://localhost:8000"  # api-gateway
  "http://localhost:8100"  # user-service
  "http://localhost:8500"  # product-service
  "http://localhost:8600"  # order-service
  "http://localhost:8400"  # payment-service
  "http://localhost:8700"  # shipping-service
  "http://localhost:8800"  # favourite-service
)

echo "🔍 Iniciando smoke tests..."
MAX_ATTEMPTS=30
SLEEP_TIME=5

for service in "${SERVICES[@]}"; do
  echo "⏳ Esperando a $service..."
  attempt=1
  while [ $attempt -le $MAX_ATTEMPTS ]; do
    if curl -f -s -o /dev/null "$service/actuator/health" 2>/dev/null; then
      echo "✅ $service está activo"
      break
    fi
    if [ $attempt -eq $MAX_ATTEMPTS ]; then
      echo "❌ $service no responde después de $((MAX_ATTEMPTS * SLEEP_TIME)) segundos"
      exit 1
    fi
    sleep $SLEEP_TIME
    attempt=$((attempt + 1))
  done
done

echo "✅ Todos los servicios están operativos"