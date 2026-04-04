#!/bin/bash
#===============================================================================
# Context Scorer Load Test - Equal Distribution
#===============================================================================
#
# This script generates equal numbers of requests per profile status
# for controlled A/B testing and comparison metrics.
#
# Distribution:
# - ACTIVE (allowed - low score):    1,000 requests
# - ACTIVE (blocked - high score):   1,000 requests
# - LEARNING:                        1,000 requests
# - BLOCKED:                         1,000 requests
#
# Total: 4,000 requests
#
# Usage:
#   ./load_test_equal.sh [BASE_URL] [REQUESTS_PER_STATUS]
#
# Example:
#   ./load_test_equal.sh http://localhost:8080/admin-api 1000
#   ./load_test_equal.sh http://localhost:8080/admin-api 500
#
#===============================================================================

set -e

# Configuration
BASE_URL="${1:-http://localhost:8080/admin-api}"
REQUESTS_PER_STATUS="${2:-1000}"
ENDPOINT="$BASE_URL/vendor/commission/1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Counters per category
ACTIVE_ALLOWED=0
ACTIVE_BLOCKED=0
LEARNING_COUNT=0
BLOCKED_COUNT=0
ERROR_COUNT=0

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

progress_bar() {
    local current=$1
    local total=$2
    local width=40
    local percentage=$((current * 100 / total))
    local filled=$((current * width / total))
    local empty=$((width - filled))

    printf "\r  [%${filled}s%${empty}s] %d/%d (%d%%)" \
        "$(printf '%*s' "$filled" | tr ' ' '█')" \
        "$(printf '%*s' "$empty" | tr ' ' '░')" \
        "$current" "$total" "$percentage"
}

send_requests_with_progress() {
    local count="$1"
    local user_id="$2"
    local device="$3"
    local ip="$4"
    local agent="$5"
    local counter_name="$6"

    for ((i=1; i<=count; i++)); do
        local response
        response=$(curl -s -o /dev/null -w "%{http_code}" "$ENDPOINT" \
            -H "X-User-Id: $user_id" \
            -H "X-Device-Id: $device" \
            -H "X-Forwarded-For: $ip" \
            -H "User-Agent: $agent" 2>/dev/null)

        case $response in
            200|201|204)
                case $counter_name in
                    "ACTIVE_ALLOWED") ((ACTIVE_ALLOWED++)) ;;
                    "LEARNING") ((LEARNING_COUNT++)) ;;
                esac
                ;;
            403)
                case $counter_name in
                    "ACTIVE_BLOCKED") ((ACTIVE_BLOCKED++)) ;;
                    "BLOCKED") ((BLOCKED_COUNT++)) ;;
                    *) ((ERROR_COUNT++)) ;;
                esac
                ;;
            *)
                ((ERROR_COUNT++))
                ;;
        esac

        # Update progress every 10 requests
        if ((i % 10 == 0)) || ((i == count)); then
            progress_bar $i $count
        fi
    done
    echo ""
}

#===============================================================================
# Main Script
#===============================================================================

TOTAL_REQUESTS=$((REQUESTS_PER_STATUS * 4))

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║      Context Scorer Load Test - Equal Distribution            ║${NC}"
echo -e "${GREEN}║                  Controlled A/B Testing                        ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Configuration:"
echo "  Target URL:           $BASE_URL"
echo "  Requests per status:  $REQUESTS_PER_STATUS"
echo "  Total requests:       $TOTAL_REQUESTS"
echo "  Start time:           $(date '+%Y-%m-%d %H:%M:%S')"
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

START_TIME=$(date +%s)

#-------------------------------------------------------------------------------
# Status 1: ACTIVE - Allowed (Low Score)
#-------------------------------------------------------------------------------
print_header "Status 1/4: ACTIVE - Allowed (Low Score)"
echo -e "  ${CYAN}admin-001 with all trusted features${NC}"
echo ""
send_requests_with_progress $REQUESTS_PER_STATUS "admin-001" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "ACTIVE_ALLOWED"

#-------------------------------------------------------------------------------
# Status 2: ACTIVE - Blocked (High Score)
#-------------------------------------------------------------------------------
print_header "Status 2/4: ACTIVE - Blocked (High Score)"
echo -e "  ${CYAN}admin-001 with all untrusted features${NC}"
echo ""
send_requests_with_progress $REQUESTS_PER_STATUS "admin-001" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$UNTRUSTED_AGENT" "ACTIVE_BLOCKED"

#-------------------------------------------------------------------------------
# Status 3: LEARNING
#-------------------------------------------------------------------------------
print_header "Status 3/4: LEARNING Profile"
echo -e "  ${CYAN}admin-003 (always allowed, data collection)${NC}"
echo ""
send_requests_with_progress $REQUESTS_PER_STATUS "admin-003" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$UNTRUSTED_AGENT" "LEARNING"

#-------------------------------------------------------------------------------
# Status 4: BLOCKED
#-------------------------------------------------------------------------------
print_header "Status 4/4: BLOCKED Profile"
echo -e "  ${CYAN}admin-004 (always denied, lockdown mode)${NC}"
echo ""
send_requests_with_progress $REQUESTS_PER_STATUS "admin-004" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "BLOCKED"

#===============================================================================
# Summary Report
#===============================================================================
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

print_header "Load Test Summary"

echo ""
echo "  Duration: ${DURATION}s"
echo ""
echo "  ┌───────────────────────────────────────────────────────┐"
echo "  │              Request Distribution                     │"
echo "  ├───────────────────────────────────────────────────────┤"
printf "  │  ACTIVE (Allowed - Low Score):     %6d requests   │\n" $ACTIVE_ALLOWED
printf "  │  ACTIVE (Blocked - High Score):    %6d requests   │\n" $ACTIVE_BLOCKED
printf "  │  LEARNING:                         %6d requests   │\n" $LEARNING_COUNT
printf "  │  BLOCKED:                          %6d requests   │\n" $BLOCKED_COUNT
printf "  │  Errors:                           %6d requests   │\n" $ERROR_COUNT
echo "  ├───────────────────────────────────────────────────────┤"
printf "  │  TOTAL:                            %6d requests   │\n" $((ACTIVE_ALLOWED + ACTIVE_BLOCKED + LEARNING_COUNT + BLOCKED_COUNT + ERROR_COUNT))
echo "  └───────────────────────────────────────────────────────┘"
echo ""

# Requests per second
if ((DURATION > 0)); then
    RPS=$((TOTAL_REQUESTS / DURATION))
    echo "  Throughput: ~$RPS requests/second"
    echo ""
fi

echo -e "${GREEN}Load test completed successfully!${NC}"
echo ""
echo "Prometheus Queries:"
echo ""
echo "  # Sample counts per status"
echo "  context_scorer_duration_seconds_count"
echo ""
echo "  # p95 latency by status (use TABLE view)"
echo "  histogram_quantile(0.95, sum by (status, le) (context_scorer_duration_seconds_bucket))"
echo ""
echo "  # p95 in milliseconds"
echo "  histogram_quantile(0.95, sum by (status, le) (context_scorer_duration_seconds_bucket)) * 1000"
echo ""
