#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOTAL="${TOTAL:-1000}"
PARALLEL="${PARALLEL:-30}"

if ! command -v uuidgen >/dev/null 2>&1; then
  echo "uuidgen is required" >&2
  exit 1
fi

echo "[kafka-burst] BASE_URL=${BASE_URL} TOTAL=${TOTAL} PARALLEL=${PARALLEL}"

create_client() {
  local idx="$1"
  local req_id
  req_id="$(uuidgen | tr '[:upper:]' '[:lower:]')"

  curl -sS -o /dev/null -w "%{http_code}\n" \
    -X POST "${BASE_URL}/api/v1/clients" \
    -H "Content-Type: application/json" \
    -d "{\"firstName\":\"Load${idx}\",\"lastName\":\"Drill\",\"email\":\"load-${idx}-${req_id}@demo.local\",\"phone\":\"+7999000${idx}\",\"riskLevel\":\"LOW\",\"kycStatus\":\"PENDING\"}"
}

export BASE_URL
export -f create_client

seq 1 "$TOTAL" | xargs -n1 -P "$PARALLEL" -I {} bash -c 'create_client "$@"' _ {} | \
  awk '{codes[$1]++} END {for (c in codes) printf("status[%s]=%d\n", c, codes[c]); if (NR==0) print "no responses"}'

echo "[kafka-burst] done"
