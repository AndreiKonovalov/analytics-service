#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
REQUESTS="${REQUESTS:-200}"
CONCURRENCY="${CONCURRENCY:-20}"
MODE="${MODE:-naive}"

case "$MODE" in
  naive)
    ENDPOINTS=("/api/v1/analytics/clients/naive")
    ;;
  optimized)
    ENDPOINTS=("/api/v1/analytics/clients/optimized?page=0&size=20")
    ;;
  compare)
    ENDPOINTS=(
      "/api/v1/analytics/clients/naive"
      "/api/v1/analytics/clients/optimized?page=0&size=20"
    )
    ;;
  *)
    echo "Unknown MODE: $MODE (use naive|optimized|compare)" >&2
    exit 1
    ;;
 esac

for ENDPOINT in "${ENDPOINTS[@]}"; do
  echo "==> Load $BASE_URL$ENDPOINT | requests=$REQUESTS concurrency=$CONCURRENCY"

  curl -fsS -o /dev/null "$BASE_URL$ENDPOINT"

  seq 1 "$REQUESTS" | \
    xargs -n1 -P "$CONCURRENCY" -I {} \
      curl -fsS -o /dev/null "$BASE_URL$ENDPOINT" >/dev/null

  echo "<== Done $BASE_URL$ENDPOINT"
  echo
 done
