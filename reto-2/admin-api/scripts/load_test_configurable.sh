#!/bin/bash
#===============================================================================
# Context Scorer Load Test - Configurable Mode
#===============================================================================
#
# Flexible load testing script with multiple modes:
#
# Modes:
#   quick    - 100 requests per scenario (fast validation)
#   standard - 500 requests per scenario (development testing)
#   full     - 1000 requests per scenario (production experiments)
#   custom   - User-defined count per scenario
#
# Features:
#   - Parallel execution option for faster completion
#   - JVM warmup phase
#   - Detailed metrics report
#   - Export results to file
#
# Usage:
#   ./load_test_configurable.sh [MODE] [OPTIONS]
#
# Examples:
#   ./load_test_configurable.sh quick
#   ./load_test_configurable.sh full --parallel
#   ./load_test_configurable.sh custom 250
#   ./load_test_configurable.sh full --url http://localhost:8080/admin-api --export
#
#===============================================================================

set -e

# Default configuration
MODE="${1:-quick}"
BASE_URL="http://localhost:8080/admin-api"
PARALLEL=false
EXPORT_RESULTS=false
CUSTOM_COUNT=100

# Parse arguments
shift || true
while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            BASE_URL="$2"
            shift 2
            ;;
        --parallel)
            PARALLEL=true
            shift
            ;;
        --export)
            EXPORT_RESULTS=true
            shift
            ;;
        [0-9]*)
            CUSTOM_COUNT="$1"
            shift
            ;;
        *)
            shift
            ;;
    esac
done

# Set counts based on mode
case $MODE in
    quick)
        COUNT=100
        WARMUP=10
        ;;
    standard)
        COUNT=500
        WARMUP=25
        ;;
    full)
        COUNT=1000
        WARMUP=50
        ;;
    custom)
        COUNT=$CUSTOM_COUNT
        WARMUP=$((CUSTOM_COUNT / 10))
        ;;
    *)
        echo "Unknown mode: $MODE"
        echo "Available modes: quick, standard, full, custom"
        exit 1
        ;;
esac

ENDPOINT="$BASE_URL/vendor/commission/1"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Test data
TRUSTED_DEVICE="device-corporate-macbook-001"
TRUSTED_IP="192.168.1.100"
TRUSTED_AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
UNTRUSTED_DEVICE="unknown-device-xyz"
UNTRUSTED_IP="45.33.32.156"
UNTRUSTED_AGENT="python-requests/2.28.0"

# Results tracking
declare -A RESULTS
RESULTS[active_low]=0
RESULTS[active_medium]=0
RESULTS[active_high]=0
RESULTS[learning]=0
RESULTS[blocked]=0
RESULTS[errors]=0

TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
REPORT_FILE="load_test_report_${TIMESTAMP}.txt"

#===============================================================================
# Functions
#===============================================================================

log() {
    local msg="$1"
    echo -e "$msg"
    if $EXPORT_RESULTS; then
        echo -e "$msg" | sed 's/\x1b\[[0-9;]*m//g' >> "$REPORT_FILE"
    fi
}

print_header() {
    log ""
    log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    log "${BLUE}  $1${NC}"
    log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_banner() {
    log ""
    log "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    log "${GREEN}║      Context Scorer Load Test - Configurable Edition          ║${NC}"
    log "${GREEN}║              Defense in Depth Performance Testing              ║${NC}"
    log "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
    log ""
}

send_request() {
    local user_id="$1"
    local device="$2"
    local ip="$3"
    local agent="$4"

    curl -s -o /dev/null -w "%{http_code}" "$ENDPOINT" \
        -H "X-User-Id: $user_id" \
        -H "X-Device-Id: $device" \
        -H "X-Forwarded-For: $ip" \
        -H "User-Agent: $agent" 2>/dev/null
}

run_scenario() {
    local name="$1"
    local count="$2"
    local user_id="$3"
    local device="$4"
    local ip="$5"
    local agent="$6"
    local result_key="$7"

    log "  ${CYAN}$name${NC} - $count requests"

    local success=0
    local blocked=0
    local error=0

    for ((i=1; i<=count; i++)); do
        local response
        response=$(send_request "$user_id" "$device" "$ip" "$agent")

        case $response in
            200|201|204) ((success++)) ;;
            403) ((blocked++)) ;;
            *) ((error++)) ;;
        esac

        # Progress indicator
        if ((i % 50 == 0)); then
            echo -n "."
        fi
    done

    echo -e " ${GREEN}Done${NC} (success: $success, blocked: $blocked, errors: $error)"

    # Update results
    RESULTS[$result_key]=$((RESULTS[$result_key] + success + blocked))
    RESULTS[errors]=$((RESULTS[errors] + error))
}

run_scenario_parallel() {
    local name="$1"
    local count="$2"
    local user_id="$3"
    local device="$4"
    local ip="$5"
    local agent="$6"
    local result_key="$7"

    log "  ${CYAN}$name${NC} - $count requests (parallel)"

    # Use xargs for parallel execution (4 concurrent)
    seq 1 $count | xargs -P 4 -I {} curl -s -o /dev/null "$ENDPOINT" \
        -H "X-User-Id: $user_id" \
        -H "X-Device-Id: $device" \
        -H "X-Forwarded-For: $ip" \
        -H "User-Agent: $agent" 2>/dev/null

    echo -e " ${GREEN}Done${NC}"
    RESULTS[$result_key]=$count
}

run_warmup() {
    log ""
    log "${YELLOW}► Warmup Phase ($WARMUP requests)${NC}"
    log "  Warming up JVM and Redis connections..."

    for ((i=1; i<=WARMUP; i++)); do
        send_request "admin-001" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" > /dev/null
    done

    log -e "  ${GREEN}Warmup complete${NC}"
    sleep 1
}

verify_connectivity() {
    echo -n "Verifying connectivity... "
    if curl -s "$BASE_URL/vendor/ping" 2>/dev/null | grep -q "pong"; then
        echo -e "${GREEN}OK${NC}"
    else
        echo -e "${RED}FAILED${NC}"
        echo "Error: Cannot reach $BASE_URL/vendor/ping"
        exit 1
    fi
}

print_config() {
    log "Configuration:"
    log "  Mode:              $MODE"
    log "  Requests/scenario: $COUNT"
    log "  Parallel:          $PARALLEL"
    log "  Export results:    $EXPORT_RESULTS"
    log "  Target URL:        $BASE_URL"
    log "  Start time:        $(date '+%Y-%m-%d %H:%M:%S')"
    log ""
}

print_summary() {
    local duration=$1

    print_header "Test Results Summary"

    log ""
    log "  Duration: ${duration}s"
    log ""
    log "  ┌───────────────────────────────────────────────────────────┐"
    log "  │                  Scenario Results                         │"
    log "  ├───────────────────────────────────────────────────────────┤"
    printf "  │  ACTIVE - Low Score (allowed):       %6d requests     │\n" ${RESULTS[active_low]} | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  ACTIVE - Low Score (allowed):       %6d requests     │\n" ${RESULTS[active_low]}
    printf "  │  ACTIVE - Medium Score (allowed):    %6d requests     │\n" ${RESULTS[active_medium]} | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  ACTIVE - Medium Score (allowed):    %6d requests     │\n" ${RESULTS[active_medium]}
    printf "  │  ACTIVE - High Score (blocked):      %6d requests     │\n" ${RESULTS[active_high]} | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  ACTIVE - High Score (blocked):      %6d requests     │\n" ${RESULTS[active_high]}
    printf "  │  LEARNING:                           %6d requests     │\n" ${RESULTS[learning]} | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  LEARNING:                           %6d requests     │\n" ${RESULTS[learning]}
    printf "  │  BLOCKED:                            %6d requests     │\n" ${RESULTS[blocked]} | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  BLOCKED:                            %6d requests     │\n" ${RESULTS[blocked]}
    log "  ├───────────────────────────────────────────────────────────┤"

    local total=$((RESULTS[active_low] + RESULTS[active_medium] + RESULTS[active_high] + RESULTS[learning] + RESULTS[blocked]))
    printf "  │  TOTAL:                              %6d requests     │\n" $total | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  TOTAL:                              %6d requests     │\n" $total
    printf "  │  ERRORS:                             %6d              │\n" ${RESULTS[errors]} | tee -a "$REPORT_FILE" 2>/dev/null || printf "  │  ERRORS:                             %6d              │\n" ${RESULTS[errors]}
    log "  └───────────────────────────────────────────────────────────┘"

    if ((duration > 0)); then
        local rps=$((total / duration))
        log ""
        log "  Throughput: ~$rps requests/second"
    fi

    log ""
    log "${GREEN}Load test completed successfully!${NC}"

    if $EXPORT_RESULTS; then
        log ""
        log "Results exported to: $REPORT_FILE"
    fi

    log ""
    log "Prometheus Queries (use TABLE view for accuracy):"
    log ""
    log "  # Sample counts"
    log "  context_scorer_duration_seconds_count"
    log ""
    log "  # p95 latency by status"
    log "  histogram_quantile(0.95, sum by (status, le) (context_scorer_duration_seconds_bucket))"
    log ""
    log "  # p95 for ACTIVE by score level"
    log "  histogram_quantile(0.95, sum by (score_level, le) (context_scorer_active_duration_seconds_bucket))"
    log ""
}

#===============================================================================
# Main Execution
#===============================================================================

print_banner
print_config
verify_connectivity

if $EXPORT_RESULTS; then
    echo "Exporting results to: $REPORT_FILE"
    echo "Context Scorer Load Test Report" > "$REPORT_FILE"
    echo "Generated: $(date)" >> "$REPORT_FILE"
    echo "Mode: $MODE" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
fi

START_TIME=$(date +%s)

# Warmup
run_warmup

# Select run function based on parallel flag
if $PARALLEL; then
    RUN_FN="run_scenario_parallel"
else
    RUN_FN="run_scenario"
fi

#-------------------------------------------------------------------------------
# Run Test Scenarios
#-------------------------------------------------------------------------------

print_header "ACTIVE Profile Tests"
$RUN_FN "Low Score (all match)" $COUNT "admin-001" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "active_low"
$RUN_FN "Medium Score (device mismatch)" $((COUNT / 2)) "admin-001" "$UNTRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "active_medium"
$RUN_FN "High Score (attack blocked)" $((COUNT / 2)) "admin-001" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$UNTRUSTED_AGENT" "active_high"

print_header "LEARNING Profile Tests"
$RUN_FN "Learning mode (data collection)" $COUNT "admin-003" "$UNTRUSTED_DEVICE" "$UNTRUSTED_IP" "$UNTRUSTED_AGENT" "learning"

print_header "BLOCKED Profile Tests"
$RUN_FN "Blocked profile (lockdown)" $COUNT "admin-004" "$TRUSTED_DEVICE" "$TRUSTED_IP" "$TRUSTED_AGENT" "blocked"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

print_summary $DURATION
