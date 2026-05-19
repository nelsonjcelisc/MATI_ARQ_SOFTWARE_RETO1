#!/usr/bin/env bash
# STEP 3 — Run chaos / compensation stress test
# Each VU creates its own unique sticker pair at runtime (no seed CSV needed).
# Run AFTER step 2 finishes to keep Prometheus metrics separate.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT="report_chaos_$(date +%d%m%y_%H%M%S).json"

echo "Running chaos stress test..."
echo "Report will be saved to: ${REPORT}"
echo ""

docker run --rm \
  --platform linux/amd64 \
  -v "${SCRIPT_DIR}":/apps \
  artilleryio/artillery:latest \
  run --output "/apps/${REPORT}" /apps/04-saga-chaos.yml

echo ""
echo "✓ Done. Report: saga-testing/${REPORT}"
echo "  Compare results in Grafana using the time range from each test run."
