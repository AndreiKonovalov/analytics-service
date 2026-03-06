#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
WINDOWS="${WINDOWS:-6}"
REQUESTS_PER_WINDOW="${REQUESTS_PER_WINDOW:-150}"
CONCURRENCY="${CONCURRENCY:-25}"
SLEEP_BETWEEN_WINDOWS_SEC="${SLEEP_BETWEEN_WINDOWS_SEC:-15}"

ENDPOINTS=(
  "/api/v1/analytics/clients/naive"
  "/api/v1/analytics/demo/n-plus-one"
)

echo "[slow-response] BASE_URL=${BASE_URL} WINDOWS=${WINDOWS} REQUESTS_PER_WINDOW=${REQUESTS_PER_WINDOW} CONCURRENCY=${CONCURRENCY}"

for window in $(seq 1 "$WINDOWS"); do
  echo "[slow-response] Window ${window}/${WINDOWS}"

  for endpoint in "${ENDPOINTS[@]}"; do
    echo "[slow-response] -> ${endpoint}"

    seq 1 "$REQUESTS_PER_WINDOW" | \
      xargs -n1 -P "$CONCURRENCY" -I {} \
      curl -sS -o /dev/null -w "%{http_code}\n" "${BASE_URL}${endpoint}" | \
      awk '{codes[$1]++} END {for (c in codes) printf("  status[%s]=%d\n", c, codes[c]); if (NR==0) print "  no responses"}'
  done

  if [[ "$window" -lt "$WINDOWS" ]]; then
    sleep "$SLEEP_BETWEEN_WINDOWS_SEC"
  fi
done

echo "[slow-response] done"
