#!/bin/bash
#===============================================================================
# Context Scorer Load Test - Realistic Distribution
#===============================================================================
#
# This script generates realistic traffic patterns for testing the Context
# Scorer Filter metrics. The distribution simulates real-world scenarios:
#
# - 60% Normal operations (legitimate admin activity)
# - 25% Attack simulations (stolen tokens, bots)
# - 15% Edge cases (errors, blocked profiles)
#
# Total: 1,350 requests
#
# Usage:
#   ./load_test_realistic.sh [BASE_URL]
#
# Example:
#   ./load_test_realistic.sh http://localhost:8080/admin-api
#
#===============================================================================

set -e

# Configuration
BASE_URL="${1:-http://localhost:8080/admin-api}"
ENDPOINT="$BASE_URL/vendor/commission/1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_REQUESTS=0
SUCCESSFUL=0
BLOCKED=0
ERRORS=0

# Trusted values (from seed data)
TRUSTED_DEVICE="device-corporate-macbook-001"
TRUSTED_IP="192.168.1.100"
TRUSTED_AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"

# Untrusted values (attack simulation)
UNTRUSTED_DEVICE="unknown-device-xyz"
UNTRUSTED_IP="45.33.32.156"
UNTRUSTED_AGENT="python-requests/2.28.0"

#===============================================================================
# Helper Functions
#===============================================================================

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_scenario() {
    echo -e "${YELLOW}► $1${NC}"
}

send_request() {
    local user_id="$1"
    local device="$2"
    local ip="$3"
    local agent="$4"

    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" "$ENDPOINT" \
        -H "X-User-Id: $user_id" \
        -H "X-Device-Id: $device" \
        -H "X-Forwarded-For: $ip" \
        -H "User-Agent: $agent" 2>/dev/null)

    ((TOTAL_REQUESTS++))

    case $response in
        200|201|204)
            ((SUCCESSFUL++))
            ;;
        403)
            ((BLOCKED++))
            ;;
        *)
            ((ERRORS++))
            ;;
    esac
}

send_request_no_header() {
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" "$ENDPOINT" 2>/dev/null)

    ((TOTAL_REQUESTS++))

    if [[ "$response" == "401" ]]; then
        ((ERRORS++))  # Expected error
    else
        ((ERRORS++))
    fi
}

run_batch() {
    local count="$1"
    local user_id="$2"
    local device="$3"
    local ip="$4"
    local agent="$5"
    local description="$6"

    echo -n "  Sending $count requests... "

    for ((i=1; i<=count; i++)); do
        send_request "$user_id" "$device" "$ip" "$agent"
    done

    echo -e "${GREEN}Done${NC}"
}

progress_bar() {
    local current=$1
    local total=$2
    local width=50
    local percentage=$((current * 100 / total))
    local filled=$((current * width / total))
    local empty=$((width - filled))

    printf "\r  Progress: ["
    printf "%${filled}s" | tr ' ' '█'
    printf "%${empty}s" | tr ' ' '░'
    printf "] %d%%" $percentage
}

#===============================================================================
# Main Script
#===============================================================================

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║     Context Scorer Load Test - Realistic Distribution         ║${NC}"
echo -e "${GREEN}║                    Defense in Depth Demo                       ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Target: $BASE_URL"
echo "Start Time: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Verify connectivity
echo -n "Verifying connectivity... "
if curl -s "$BASE_URL/vendor/ping" | grep -q "pong"; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    echo "Error: Cannot reach $BASE_URL/vendor/ping"
    exit 1
fi

#-------------------------------------------------------------------------------
# Phase 1: Normal Operations (60% - 800 requests)
#-------------------------------------------------------------------------------
print_header "Phase 1: Normal Operations (800 requests)"

print_scenario "ACTIVE Profile - Low Score (All Features Match) - 400 requests"
run_batch 400 "admin-001" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "Low score"

print_scenario "ACTIVE Profile - Medium Score (Device Mismatch) - 200 requests"
run_batch 200 "admin-001" "$UNTRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "Medium score"

print_scenario "ACTIVE Profile - Medium Score (IP Mismatch) - 200 requests"
run_batch 200 "admin-001" "$TRUSTED_DEVICE" "$UNTRUSTED_IP" "$TRUSTED_AGENT" "Medium score"

#-------------------------------------------------------------------------------
# Phase 2: Attack Simulations (25% - 350 requests)
#-------------------------------------------------------------------------------
print_header "Phase 2: Attack Simulations (350 requests)"

print_scenario "ACTIVE Profile - High Score (Device + IP Mismatch) - 100 requests"
run_batch 100 "admin-001" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$TRUSTED_AGENT" "High score"

print_scenario "Stolen Token Attack (All Anomalous) - 150 requests"
run_batch 150 "admin-001" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$UNTRUSTED_AGENT" "Attack blocked"

print_scenario "Bot/Script Attack (Automated User-Agent) - 100 requests"
run_batch 100 "admin-001" "$TRUSTED_DEVICE" "$TRUSTED_IP" "curl/7.88.1" "Bot detected"

#-------------------------------------------------------------------------------
# Phase 3: Profile Status Tests (15% - 200 requests)
#-------------------------------------------------------------------------------
print_header "Phase 3: Profile Status Tests (200 requests)"

print_scenario "LEARNING Profile (Always Allowed) - 100 requests"
run_batch 100 "admin-003" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$UNTRUSTED_AGENT" "Learning mode"

print_scenario "BLOCKED Profile (Always Denied) - 100 requests"
run_batch 100 "admin-004" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "Blocked profile"

#-------------------------------------------------------------------------------
# Phase 4: Edge Cases (50 requests)
#-------------------------------------------------------------------------------
print_header "Phase 4: Edge Cases (50 requests)"

print_scenario "Missing X-User-Id Header - 25 requests"
echo -n "  Sending 25 requests... "
for ((i=1; i<=25; i++)); do
    send_request_no_header
done
echo -e "${GREEN}Done${NC}"

print_scenario "Unknown Admin ID (Profile Not Found) - 25 requests"
run_batch 25 "admin-unknown-999" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "Unknown admin"

#===============================================================================
# Summary Report
#===============================================================================
print_header "Load Test Summary"

END_TIME=$(date '+%Y-%m-%d %H:%M:%S')

echo ""
echo "  End Time: $END_TIME"
echo ""
echo "  ┌─────────────────────────────────────┐"
echo "  │         Request Summary             │"
echo "  ├─────────────────────────────────────┤"
printf "  │  Total Requests:    %6d          │\n" $TOTAL_REQUESTS
printf "  │  Successful (2xx):  %6d          │\n" $SUCCESSFUL
printf "  │  Blocked (403):     %6d          │\n" $BLOCKED
printf "  │  Errors (4xx/5xx):  %6d          │\n" $ERRORS
echo "  └─────────────────────────────────────┘"
echo ""
echo -e "${GREEN}Load test completed successfully!${NC}"
echo ""
echo "Next steps:"
echo "  1. Open Prometheus: http://localhost:9090"
echo "  2. Query: context_scorer_requests_total"
echo "  3. Check p95 latency: histogram_quantile(0.95, sum by (status, le) (context_scorer_duration_seconds_bucket))"
echo "  4. Use TABLE view for accurate readings"
echo ""
