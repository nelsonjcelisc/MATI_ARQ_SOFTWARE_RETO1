#!/usr/bin/env bash
# STEP 0 — Full reset: truncates all test data and removes stale seed files.
# Run this before starting a fresh test session.
# Usage: bash 00-reset.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Resetting database tables..."
docker exec postgres psql -U appuser -d reto_final_db -c "
  TRUNCATE
    exchange.exchange_state,
    collection.stickers,
    collection.albums,
    inventory.inventory_items,
    inventory.stickers,
    inventory.albums
  CASCADE;
"

echo "Removing stale run artifacts..."
rm -f \
  "${SCRIPT_DIR}/saga-run-log.jsonl" \
  "${SCRIPT_DIR}"/report_*.json

echo ""
echo "✓ Reset complete. Run the test sequence:"
echo "  bash 03-happy-run.sh"
echo "  bash 04-chaos-run.sh"
