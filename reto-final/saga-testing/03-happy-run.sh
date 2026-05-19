#!/usr/bin/env bash
# STEP 2 — Run happy path stress test
# Each VU creates its own unique sticker pair at runtime (no seed CSV needed).
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT="report_happy_$(date +%d%m%y_%H%M%S).json"

echo "Running happy path stress test..."
echo "Report will be saved to: ${REPORT}"
echo ""

docker run --rm \
  --platform linux/amd64 \
  -v "${SCRIPT_DIR}":/apps \
  artilleryio/artillery:latest \
  run --output "/apps/${REPORT}" /apps/03-saga-happy.yml

echo ""
echo "✓ Done. Report: saga-testing/${REPORT}"
echo "  Next step: bash 04-chaos-run.sh"
