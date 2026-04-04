# Context Scorer Metrics - Testing Guide

This guide provides step-by-step instructions for testing the Context Scorer Filter metrics implementation, including HTTP requests and Prometheus queries.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Quick Start](#2-quick-start)
3. [Test Scenarios by Profile Status](#3-test-scenarios-by-profile-status)
4. [Prometheus Queries Reference](#4-prometheus-queries-reference)
5. [Expected Metrics Behavior](#5-expected-metrics-behavior)
6. [Dashboard Panels](#6-dashboard-panels)
7. [Load Testing Script](#7-load-testing-script)
8. [Troubleshooting](#8-troubleshooting)
9. [Important Lessons Learned](#9-important-lessons-learned)
10. [Summary](#10-summary)

---

## 1. Prerequisites

### Start Infrastructure

```bash
# From reto-2 directory
cd /path/to/MATI_ARQ_SOFTWARE_RETO1/reto-2

# Start PostgreSQL, Redis, and Prometheus
docker-compose up -d postgres redis prometheus

# Wait for services to be healthy
docker-compose ps
```

> **Note:** If you modify `infra/prometheus/prometheus.yml`, you must restart Prometheus:
> ```bash
> docker compose restart prometheus
> ```

### Start Admin API

```bash
cd admin-api
mvn spring-boot:run
```

### Verify Endpoints

```bash
# Health check (no auth required)
curl http://localhost:8080/admin-api/vendor/ping
# Expected: pong

# Prometheus metrics endpoint
curl http://localhost:8080/admin-api/actuator/prometheus | grep context_scorer
```

### Access Points

| Service | URL |
|---------|-----|
| Admin API | http://localhost:8080/admin-api |
| Prometheus UI | http://localhost:9090 |
| Metrics Endpoint | http://localhost:8080/admin-api/actuator/prometheus |

---

## 2. Quick Start

> **Important:** The Quick Start generates only 1 sample per scenario. This is useful for verifying metrics are working, but **NOT sufficient for accurate percentile analysis**. See [Section 9.1](#91-minimum-sample-size-for-accurate-percentiles) for generating enough samples.

Run these commands to generate metrics across all scenarios:

```bash
# Set base URL
BASE_URL="http://localhost:8080/admin-api"

# 1. Health check (skipped - excluded path)
curl -s "$BASE_URL/vendor/ping"

# 2. Missing header (401 - error counter)
curl -s "$BASE_URL/vendor/commission/1"

# 3. ACTIVE profile - Score 0% (all match)
curl -s "$BASE_URL/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: device-corporate-macbook-001" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"

# 4. ACTIVE profile - Score 100% (blocked)
curl -s "$BASE_URL/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: unknown-device" \
  -H "X-Forwarded-For: 45.33.32.156" \
  -H "User-Agent: python-requests/2.28.0"

# 5. LEARNING profile (always allowed)
curl -s "$BASE_URL/vendor/commission/1" \
  -H "X-User-Id: admin-003" \
  -H "X-Device-Id: unknown-device" \
  -H "X-Forwarded-For: 45.33.32.156" \
  -H "User-Agent: python-requests/2.28.0"

# 6. BLOCKED profile (always denied)
curl -s "$BASE_URL/vendor/commission/1" \
  -H "X-User-Id: admin-004" \
  -H "X-Device-Id: device-corporate-macbook-001" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
```

Then open Prometheus UI and run:
```promql
context_scorer_requests_total
```

---

## 3. Test Scenarios by Profile Status

### 3.1 ACTIVE Profile Tests

#### Test A1: Score 0% - All Features Match (ALLOWED)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: device-corporate-macbook-001" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
```

**Expected Response:** `200 OK` with commission data

**Scoring Breakdown:**
| Feature | Value | Match | Weight |
|---------|-------|-------|--------|
| Device | device-corporate-macbook-001 | YES | 0.00 |
| IP | 192.168.1.100 | YES | 0.00 |
| Hour | (current hour 7-18) | YES | 0.00 |
| User-Agent | Mozilla/5.0... | YES | 0.00 |
| **Total** | | | **0.00** |

**Expected Metrics:**
```promql
# Counter increment
context_scorer_requests_total{status="ACTIVE", decision="ALLOWED"} # +1

# Duration recorded
context_scorer_duration_seconds_count{status="ACTIVE"} # +1

# Score distribution
context_scorer_active_score # records 0.0

# Active duration by score level
context_scorer_active_duration_seconds_count{score_level="low", decision="ALLOWED"} # +1
```

---

#### Test A2: Score 30% - Device Mismatch (ALLOWED)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: unknown-device-xyz" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
```

**Expected Response:** `200 OK`

**Scoring Breakdown:**
| Feature | Value | Match | Weight |
|---------|-------|-------|--------|
| Device | unknown-device-xyz | NO | +0.30 |
| IP | 192.168.1.100 | YES | 0.00 |
| Hour | (current hour 7-18) | YES | 0.00 |
| User-Agent | Mozilla/5.0... | YES | 0.00 |
| **Total** | | | **0.30** |

**Expected Metrics:**
```promql
context_scorer_requests_total{status="ACTIVE", decision="ALLOWED"} # +1
context_scorer_active_duration_seconds_count{score_level="low", decision="ALLOWED"} # +1
# (score 0.30 < 0.35 threshold for "low")
```

---

#### Test A3: Score 50% - Device + Agent Mismatch (ALLOWED)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: unknown-device-xyz" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: python-requests/2.28.0"
```

**Expected Response:** `200 OK`

**Scoring Breakdown:**
| Feature | Value | Match | Weight |
|---------|-------|-------|--------|
| Device | unknown-device-xyz | NO | +0.30 |
| IP | 192.168.1.100 | YES | 0.00 |
| Hour | (current hour 7-18) | YES | 0.00 |
| User-Agent | python-requests/2.28.0 | NO | +0.20 |
| **Total** | | | **0.50** |

**Expected Metrics:**
```promql
context_scorer_requests_total{status="ACTIVE", decision="ALLOWED"} # +1
context_scorer_active_duration_seconds_count{score_level="medium", decision="ALLOWED"} # +1
# (score 0.50 >= 0.35 and < 0.70, so "medium")
```

---

#### Test A4: Score 60% - Device + IP Mismatch (ALLOWED - Just Under Threshold)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: unknown-device-xyz" \
  -H "X-Forwarded-For: 45.33.32.156" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
```

**Expected Response:** `200 OK`

**Scoring Breakdown:**
| Feature | Value | Match | Weight |
|---------|-------|-------|--------|
| Device | unknown-device-xyz | NO | +0.30 |
| IP | 45.33.32.156 | NO | +0.30 |
| Hour | (current hour 7-18) | YES | 0.00 |
| User-Agent | Mozilla/5.0... | YES | 0.00 |
| **Total** | | | **0.60** |

**Expected Metrics:**
```promql
context_scorer_requests_total{status="ACTIVE", decision="ALLOWED"} # +1
context_scorer_active_duration_seconds_count{score_level="medium", decision="ALLOWED"} # +1
```

---

#### Test A5: Score 80% - Device + IP + Agent Mismatch (BLOCKED)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: unknown-device-xyz" \
  -H "X-Forwarded-For: 45.33.32.156" \
  -H "User-Agent: python-requests/2.28.0"
```

**Expected Response:** `403 Forbidden`
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied due to anomalous context",
  "score": 0.80,
  "reason": "Anomaly score 0.80 exceeds threshold 0.70"
}
```

**Scoring Breakdown:**
| Feature | Value | Match | Weight |
|---------|-------|-------|--------|
| Device | unknown-device-xyz | NO | +0.30 |
| IP | 45.33.32.156 | NO | +0.30 |
| Hour | (current hour 7-18) | YES | 0.00 |
| User-Agent | python-requests/2.28.0 | NO | +0.20 |
| **Total** | | | **0.80** |

**Expected Metrics:**
```promql
context_scorer_requests_total{status="ACTIVE", decision="BLOCKED"} # +1
context_scorer_active_duration_seconds_count{score_level="high", decision="BLOCKED"} # +1
# (score 0.80 >= 0.70 threshold, so "high")
```

---

#### Test A6: Score 100% - All Features Anomalous (BLOCKED)

**Request:** (Run outside business hours 7-18 for full 100%)
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-001" \
  -H "X-Device-Id: attacker-device-666" \
  -H "X-Forwarded-For: 185.220.101.1" \
  -H "User-Agent: curl/7.88.1"
```

**Expected Response:** `403 Forbidden`
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied due to anomalous context",
  "score": 1.00,
  "reason": "Anomaly score 1.00 exceeds threshold 0.70"
}
```

---

### 3.2 LEARNING Profile Tests

#### Test L1: LEARNING - All Anomalous (Still ALLOWED)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-003" \
  -H "X-Device-Id: unknown-device-xyz" \
  -H "X-Forwarded-For: 45.33.32.156" \
  -H "User-Agent: python-requests/2.28.0"
```

**Expected Response:** `200 OK` (LEARNING mode bypasses threshold)

**Expected Metrics:**
```promql
context_scorer_requests_total{status="LEARNING", decision="ALLOWED"} # +1
context_scorer_duration_seconds_count{status="LEARNING"} # +1
# Note: No active_duration metrics for LEARNING (only for ACTIVE)
```

---

### 3.3 BLOCKED Profile Tests

#### Test B1: BLOCKED - All Trusted (Still BLOCKED)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-004" \
  -H "X-Device-Id: device-corporate-macbook-001" \
  -H "X-Forwarded-For: 192.168.1.100" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
```

**Expected Response:** `403 Forbidden`
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied due to anomalous context",
  "score": 1.00,
  "reason": "Admin profile is blocked"
}
```

**Expected Metrics:**
```promql
context_scorer_requests_total{status="BLOCKED", decision="BLOCKED"} # +1
context_scorer_duration_seconds_count{status="BLOCKED"} # +1
```

---

### 3.4 Error Cases

#### Test E1: Missing X-User-Id Header (401)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1"
```

**Expected Response:** `401 Unauthorized`

**Expected Metrics:**
```promql
context_scorer_errors_total{reason="missing_header"} # +1
```

---

#### Test E2: Unknown Admin ID (403 - Profile Not Found)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/commission/1" \
  -H "X-User-Id: admin-unknown-999"
```

**Expected Response:** `403 Forbidden`
```json
{
  "reason": "Profile not found"
}
```

**Expected Metrics:**
```promql
context_scorer_requests_total{status="UNKNOWN", decision="BLOCKED"} # +1
context_scorer_errors_total{reason="profile_not_found"} # +1
```

---

### 3.5 Skipped Requests

#### Test S1: Health Check (Excluded Path)

**Request:**
```bash
curl -v "http://localhost:8080/admin-api/vendor/ping"
```

**Expected Response:** `200 OK` - `pong`

**Expected Metrics:**
```promql
context_scorer_skipped_total{reason="excluded_path"} # +1
```

---

## 4. Prometheus Queries Reference

### 4.1 Request Counts

```promql
# Total requests by status and decision
context_scorer_requests_total

# Request rate by status (last 5 minutes)
rate(context_scorer_requests_total[5m])

# Requests grouped by status only
sum by (status) (context_scorer_requests_total)

# Requests grouped by decision only
sum by (decision) (context_scorer_requests_total)

# Block rate for ACTIVE profiles
sum(rate(context_scorer_requests_total{status="ACTIVE",decision="BLOCKED"}[5m]))
/ sum(rate(context_scorer_requests_total{status="ACTIVE"}[5m]))
```

### 4.2 Duration Metrics (Latency)

```promql
# p50 latency by status
histogram_quantile(0.50, sum by (status, le) (rate(context_scorer_duration_seconds_bucket[5m])))

# p95 latency by status
histogram_quantile(0.95, sum by (status, le) (rate(context_scorer_duration_seconds_bucket[5m])))

# p99 latency by status
histogram_quantile(0.99, sum by (status, le) (rate(context_scorer_duration_seconds_bucket[5m])))

# Average duration by status
sum by (status) (rate(context_scorer_duration_seconds_sum[5m]))
/ sum by (status) (rate(context_scorer_duration_seconds_count[5m]))
```

### 4.3 ACTIVE Profile Deep Dive (Score Levels)

```promql
# p95 latency for ACTIVE profiles by score level
histogram_quantile(0.95,
  sum by (score_level, le) (rate(context_scorer_active_duration_seconds_bucket[5m]))
)

# Compare high vs low score evaluation time
histogram_quantile(0.95,
  sum by (score_level, decision, le) (rate(context_scorer_active_duration_seconds_bucket[5m]))
)

# Request count by score level
sum by (score_level) (context_scorer_active_duration_seconds_count)

# Score distribution percentiles
context_scorer_active_score{quantile="0.50"}  # Median score
context_scorer_active_score{quantile="0.95"}  # p95 score
context_scorer_active_score{quantile="0.99"}  # p99 score
```

### 4.4 Error Metrics

```promql
# All errors by reason
context_scorer_errors_total

# Error rate
rate(context_scorer_errors_total[5m])

# Missing header errors
context_scorer_errors_total{reason="missing_header"}

# Profile not found errors
context_scorer_errors_total{reason="profile_not_found"}
```

### 4.5 Skipped Requests

```promql
# Skipped requests by reason
context_scorer_skipped_total

# Skipped due to excluded paths
context_scorer_skipped_total{reason="excluded_path"}

# Skipped because filter disabled
context_scorer_skipped_total{reason="disabled"}
```

---

## 5. Expected Metrics Behavior

### 5.1 Metrics After Running All Tests

After running all test scenarios, you should see:

```promql
# Request counts
context_scorer_requests_total{status="ACTIVE", decision="ALLOWED"}    >= 4
context_scorer_requests_total{status="ACTIVE", decision="BLOCKED"}    >= 2
context_scorer_requests_total{status="LEARNING", decision="ALLOWED"}  >= 1
context_scorer_requests_total{status="BLOCKED", decision="BLOCKED"}   >= 1
context_scorer_requests_total{status="UNKNOWN", decision="BLOCKED"}   >= 1

# Errors
context_scorer_errors_total{reason="missing_header"}    >= 1
context_scorer_errors_total{reason="profile_not_found"} >= 1

# Skipped
context_scorer_skipped_total{reason="excluded_path"} >= 1
```

### 5.2 Score Level Distribution

For ACTIVE profiles:

| Score Range | Score Level | Typical Scenarios |
|-------------|-------------|-------------------|
| 0.00 - 0.34 | `low` | All features match, or 1 low-weight mismatch |
| 0.35 - 0.69 | `medium` | 2 mismatches, approaching threshold |
| 0.70 - 1.00 | `high` | Blocked - 3+ mismatches or high-weight combination |

### 5.3 Performance Expectations

> **Important:** These values are only accurate with **50+ samples per status**. With fewer samples, JVM warmup and random variance will dominate. See [Section 9.1](#91-minimum-sample-size-for-accurate-percentiles).

| Profile Status | Expected p95 Latency | Processing | Notes |
|----------------|---------------------|------------|-------|
| **BLOCKED** | **~3 ms** | Status check only | Fastest - early exit, no scoring |
| **LEARNING** | **~8 ms** | Full scoring | Medium - scores but no threshold check |
| **ACTIVE** | **~11 ms** | Full scoring + threshold | Slowest - complete evaluation |
| UNKNOWN | ~3-5 ms | Profile lookup fails | Early exit on profile not found |

**Expected order (fastest to slowest):**
```
BLOCKED < LEARNING < ACTIVE
```

**Visual comparison:**
```
BLOCKED    ███          (~3ms)   - Early exit after status check
LEARNING   ████████     (~8ms)   - 4 Redis lookups for features
ACTIVE     ███████████  (~11ms)  - 4 Redis lookups + threshold logic
```

---

## 6. Dashboard Panels

### 6.1 Suggested Grafana Panels

#### Panel 1: Request Rate by Status
```promql
sum by (status) (rate(context_scorer_requests_total[5m]))
```
Type: Time series

#### Panel 2: Block Rate (ACTIVE profiles)
```promql
sum(rate(context_scorer_requests_total{status="ACTIVE",decision="BLOCKED"}[5m]))
/ sum(rate(context_scorer_requests_total{status="ACTIVE"}[5m])) * 100
```
Type: Gauge (0-100%)

#### Panel 3: p95 Latency by Status
```promql
histogram_quantile(0.95, sum by (status, le) (rate(context_scorer_duration_seconds_bucket[5m])))
```
Type: Time series

#### Panel 4: ACTIVE Latency by Score Level
```promql
histogram_quantile(0.95, sum by (score_level, le) (rate(context_scorer_active_duration_seconds_bucket[5m])))
```
Type: Time series

#### Panel 5: Score Distribution
```promql
context_scorer_active_score{quantile="0.50"}
context_scorer_active_score{quantile="0.95"}
context_scorer_active_score{quantile="0.99"}
```
Type: Stat panel

#### Panel 6: Error Rate
```promql
sum by (reason) (rate(context_scorer_errors_total[5m]))
```
Type: Time series

---

## 7. Load Testing Script

Generate metrics under load:

```bash
#!/bin/bash
# load_test.sh - Generate metrics for Context Scorer

BASE_URL="http://localhost:8080/admin-api"
ITERATIONS=100

echo "Starting load test with $ITERATIONS iterations per scenario..."

# ACTIVE - Allowed (Score 0%)
echo "Testing ACTIVE - Score 0% (Allowed)..."
for i in $(seq 1 $ITERATIONS); do
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-001" \
    -H "X-Device-Id: device-corporate-macbook-001" \
    -H "X-Forwarded-For: 192.168.1.100" \
    -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)" > /dev/null
done
echo "Done: $ITERATIONS requests"

# ACTIVE - Allowed (Score 50%)
echo "Testing ACTIVE - Score 50% (Allowed)..."
for i in $(seq 1 $ITERATIONS); do
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-001" \
    -H "X-Device-Id: unknown-device" \
    -H "X-Forwarded-For: 192.168.1.100" \
    -H "User-Agent: python-requests/2.28.0" > /dev/null
done
echo "Done: $ITERATIONS requests"

# ACTIVE - Blocked (Score 80%)
echo "Testing ACTIVE - Score 80% (Blocked)..."
for i in $(seq 1 $ITERATIONS); do
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-001" \
    -H "X-Device-Id: unknown-device" \
    -H "X-Forwarded-For: 45.33.32.156" \
    -H "User-Agent: python-requests/2.28.0" > /dev/null
done
echo "Done: $ITERATIONS requests"

# LEARNING
echo "Testing LEARNING profile..."
for i in $(seq 1 $ITERATIONS); do
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-003" \
    -H "X-Device-Id: unknown-device" \
    -H "X-Forwarded-For: 45.33.32.156" \
    -H "User-Agent: python-requests/2.28.0" > /dev/null
done
echo "Done: $ITERATIONS requests"

# BLOCKED
echo "Testing BLOCKED profile..."
for i in $(seq 1 $ITERATIONS); do
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-004" \
    -H "X-Device-Id: device-corporate-macbook-001" \
    -H "X-Forwarded-For: 192.168.1.100" \
    -H "User-Agent: Mozilla/5.0" > /dev/null
done
echo "Done: $ITERATIONS requests"

echo ""
echo "Load test complete! Check Prometheus at http://localhost:9090"
echo "Try: context_scorer_requests_total"
```

---

## 8. Troubleshooting

### Metrics Not Appearing

1. **Check actuator endpoint:**
   ```bash
   curl http://localhost:8080/admin-api/actuator/prometheus | grep context_scorer
   ```

2. **Check Prometheus targets:**
   - Go to http://localhost:9090/targets
   - Verify `admin-api` target is UP

3. **Check application logs:**
   ```bash
   # Look for ContextScorerMetrics initialization
   grep "ContextScorerMetrics" logs/application.log
   ```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No metrics | Filter disabled | Check `security.context-scorer.enabled: true` |
| Missing status label | Profile not found | Verify Redis has profiles loaded |
| No active_duration | Not ACTIVE profile | Only ACTIVE profiles record score level metrics |
| Skipped metrics high | Many health checks | Normal - actuator/health excluded |
| Empty query results | Prometheus not scraping | Restart Prometheus: `docker compose restart prometheus` |
| Wrong latency order | Too few samples | Generate 50+ samples per status before analyzing |
| Graph looks wrong | UI visualization issue | Use **Table** view instead of Graph view |
| BLOCKED slower than ACTIVE | Sample size < 10 | JVM warmup noise; generate more samples |

---

## 9. Important Lessons Learned

### 9.1 Minimum Sample Size for Accurate Percentiles

**Problem:** With only 1-2 samples per status, p95/p99 percentiles show random noise instead of actual performance characteristics.

**Why it happens:**
- First requests are slower due to JVM warmup
- Initial Redis connections have higher latency
- Random system variance dominates with few samples

**Solution:** Generate at least **50+ samples per status** before analyzing percentiles.

```bash
# Generate sufficient samples for accurate metrics
BASE_URL="http://localhost:8080/admin-api"

for i in {1..50}; do
  # ACTIVE
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-001" \
    -H "X-Device-Id: device-corporate-macbook-001" \
    -H "X-Forwarded-For: 192.168.1.100" \
    -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)" > /dev/null

  # LEARNING
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-003" \
    -H "X-Device-Id: unknown" \
    -H "X-Forwarded-For: 45.33.32.156" \
    -H "User-Agent: python-requests/2.28.0" > /dev/null

  # BLOCKED
  curl -s "$BASE_URL/vendor/commission/1" \
    -H "X-User-Id: admin-004" \
    -H "X-Device-Id: device-corporate-macbook-001" \
    -H "X-Forwarded-For: 192.168.1.100" \
    -H "User-Agent: Mozilla/5.0" > /dev/null
done

echo "Generated 50 samples per status"
```

**Verify sample counts:**
```promql
context_scorer_duration_seconds_count
```

You should see 50+ for each status before trusting percentile values.

---

### 9.2 Use Table View Instead of Graph View

**Problem:** Prometheus Graph view with stacked area charts can be visually misleading - the vertical position doesn't indicate higher/lower values.

**Solution:** Use **Table view** for accurate metric readings.

**Steps:**
1. Go to Prometheus UI: `http://localhost:9090`
2. Enter your query (e.g., `histogram_quantile(0.95, sum by (status, le) (context_scorer_duration_seconds_bucket))`)
3. Click the **"Table"** tab instead of "Graph"
4. Values are shown clearly sorted

**Pro tip:** To see values in milliseconds instead of seconds:
```promql
histogram_quantile(0.95, sum by (status, le) (context_scorer_duration_seconds_bucket)) * 1000
```

---

### 9.3 Expected Latency Order

After sufficient samples, the expected p95 latency order is:

```
BLOCKED  < LEARNING < ACTIVE
(fastest)            (slowest)
```

| Status | Expected p95 | Processing Steps |
|--------|--------------|------------------|
| **BLOCKED** | ~3 ms | Redis status check → Early exit (no scoring) |
| **LEARNING** | ~8 ms | Redis status check → 4 feature checks → Return |
| **ACTIVE** | ~11 ms | Redis status check → 4 feature checks → Threshold comparison → Return |

**Why BLOCKED is fastest:**
```java
// In ContextScoringService.evaluateContext():
if (status == AdminProfileStatus.BLOCKED) {
    // IMMEDIATE RETURN - no score calculation!
    return ScoringResult.builder()
        .decision(Decision.BLOCKED)
        .reason("Admin profile is blocked")
        .build();
}
```

**Visual representation:**
```
BLOCKED    ███          (~3ms)
LEARNING   ████████     (~8ms)
ACTIVE     ███████████  (~11ms)
```

---

### 9.4 Restart Prometheus After Config Changes

**Problem:** After updating `prometheus.yml` to add new scrape targets, Prometheus shows empty results because it's using the old configuration.

**Solution:** Restart Prometheus to load the new configuration.

```bash
# Using docker compose
docker compose restart prometheus

# Or using docker directly
docker restart prometheus
```

**Verify the target is UP:**
```bash
curl -s http://localhost:9090/api/v1/targets | grep admin-api
```

Or check in the UI: `http://localhost:9090/targets`

---

### 9.5 Verifying Metrics via API vs UI

If the Prometheus UI shows unexpected results, verify directly via API:

```bash
# Check p95 latency via API (more reliable than UI graph)
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,sum%20by%20(status,le)(context_scorer_duration_seconds_bucket))' | python3 -m json.tool
```

**Expected output (with correct latency order):**
```json
{
  "status": "success",
  "data": {
    "result": [
      {"metric": {"status": "BLOCKED"}, "value": [1234567890, "0.003"]},
      {"metric": {"status": "LEARNING"}, "value": [1234567890, "0.008"]},
      {"metric": {"status": "ACTIVE"}, "value": [1234567890, "0.011"]}
    ]
  }
}
```

---

## 10. Summary

| Metric | Purpose | Key Labels |
|--------|---------|------------|
| `context_scorer_requests_total` | Request count | `status`, `decision` |
| `context_scorer_duration_seconds` | Evaluation latency by status | `status` |
| `context_scorer_active_score` | Score distribution (ACTIVE only) | quantiles |
| `context_scorer_active_duration_seconds` | Latency by score level (ACTIVE only) | `score_level`, `decision` |
| `context_scorer_errors_total` | Error count | `reason` |
| `context_scorer_skipped_total` | Skipped request count | `reason` |

This metrics implementation provides full visibility into Context Scorer Filter performance across all profile statuses, with deep insights into ACTIVE profile scoring behavior.
