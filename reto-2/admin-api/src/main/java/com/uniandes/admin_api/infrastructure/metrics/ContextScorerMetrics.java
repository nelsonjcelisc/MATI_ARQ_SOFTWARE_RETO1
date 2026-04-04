package com.uniandes.admin_api.infrastructure.metrics;

import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import com.uniandes.admin_api.domain.model.Decision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.DistributionSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prometheus metrics for Context Scorer Filter performance monitoring.
 *
 * Provides detailed metrics for:
 * - Request counts by profile status (LEARNING, ACTIVE, BLOCKED) and decision
 * - Evaluation duration by profile status
 * - For ACTIVE profiles: score distribution and evaluation time by score level (high/low)
 *
 * Metrics exposed:
 * - context_scorer_requests_total: Counter of requests by status and decision
 * - context_scorer_duration_seconds: Timer for evaluation duration by status
 * - context_scorer_active_score: Distribution summary of scores for ACTIVE profiles
 * - context_scorer_active_duration_seconds: Timer for ACTIVE evaluations by score level
 */
@Slf4j
@Component
public class ContextScorerMetrics {

    private static final String METRIC_PREFIX = "context_scorer";
    private static final String TAG_STATUS = "status";
    private static final String TAG_DECISION = "decision";
    private static final String TAG_SCORE_LEVEL = "score_level";
    private static final String TAG_REASON = "reason";

    private final MeterRegistry registry;

    // Cached counters for performance
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> durationTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> activeDurationTimers = new ConcurrentHashMap<>();

    // Score distribution for ACTIVE profiles
    private final DistributionSummary activeScoreDistribution;

    // Counters for quick access
    private final Counter profileNotFoundCounter;
    private final Counter missingHeaderCounter;

    public ContextScorerMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Score distribution for ACTIVE profiles
        this.activeScoreDistribution = DistributionSummary.builder(METRIC_PREFIX + "_active_score")
                .description("Distribution of anomaly scores for ACTIVE profile evaluations")
                .baseUnit("score")
                .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        // Special case counters
        this.profileNotFoundCounter = Counter.builder(METRIC_PREFIX + "_errors_total")
                .description("Total errors during context scoring")
                .tag(TAG_REASON, "profile_not_found")
                .register(registry);

        this.missingHeaderCounter = Counter.builder(METRIC_PREFIX + "_errors_total")
                .description("Total errors during context scoring")
                .tag(TAG_REASON, "missing_header")
                .register(registry);

        log.info("ContextScorerMetrics initialized with Prometheus registry");
    }

    /**
     * Records a complete scoring evaluation with all relevant metrics.
     *
     * @param status      Profile status (LEARNING, ACTIVE, BLOCKED, or null if not found)
     * @param decision    The decision (ALLOWED, BLOCKED)
     * @param score       The calculated anomaly score
     * @param durationMs  Evaluation duration in milliseconds
     * @param threshold   The configured threshold (for determining high/low score)
     */
    public void recordEvaluation(AdminProfileStatus status, Decision decision,
                                  double score, long durationMs, double threshold) {
        String statusTag = status != null ? status.name() : "UNKNOWN";
        String decisionTag = decision.name();

        // Record request count
        getOrCreateRequestCounter(statusTag, decisionTag).increment();

        // Record duration by status
        getOrCreateDurationTimer(statusTag).record(Duration.ofMillis(durationMs));

        // For ACTIVE profiles, record detailed metrics
        if (status == AdminProfileStatus.ACTIVE) {
            recordActiveEvaluation(score, durationMs, threshold, decision);
        }

        log.debug("Recorded metrics - status: {}, decision: {}, score: {:.2f}, duration: {}ms",
                statusTag, decisionTag, score, durationMs);
    }

    /**
     * Records detailed metrics for ACTIVE profile evaluations.
     * This allows tracking how long it takes to determine high vs low scores.
     */
    private void recordActiveEvaluation(double score, long durationMs,
                                         double threshold, Decision decision) {
        // Record score distribution
        activeScoreDistribution.record(score);

        // Determine score level based on threshold
        String scoreLevel = determineScoreLevel(score, threshold);

        // Record duration by score level
        getOrCreateActiveDurationTimer(scoreLevel, decision.name())
                .record(Duration.ofMillis(durationMs));
    }

    /**
     * Determines the score level category for ACTIVE profile evaluations.
     *
     * Categories:
     * - low: score < 50% of threshold (clearly safe)
     * - medium: score between 50% and 100% of threshold (approaching risk)
     * - high: score >= threshold (blocked)
     */
    private String determineScoreLevel(double score, double threshold) {
        double halfThreshold = threshold / 2.0;

        if (score >= threshold) {
            return "high";       // Blocked - score exceeded threshold
        } else if (score >= halfThreshold) {
            return "medium";     // Approaching threshold
        } else {
            return "low";        // Clearly safe
        }
    }

    /**
     * Records a missing X-User-Id header incident.
     */
    public void recordMissingHeader() {
        missingHeaderCounter.increment();
    }

    /**
     * Records a profile not found incident.
     */
    public void recordProfileNotFound() {
        profileNotFoundCounter.increment();
    }

    /**
     * Records when scorer is disabled/skipped.
     */
    public void recordSkipped(String reason) {
        Counter.builder(METRIC_PREFIX + "_skipped_total")
                .description("Total requests skipped by context scorer")
                .tag(TAG_REASON, reason)
                .register(registry)
                .increment();
    }

    // ==================== Helper methods for lazy metric creation ====================

    private Counter getOrCreateRequestCounter(String status, String decision) {
        String key = status + "_" + decision;
        return requestCounters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_PREFIX + "_requests_total")
                        .description("Total requests processed by context scorer")
                        .tag(TAG_STATUS, status)
                        .tag(TAG_DECISION, decision)
                        .register(registry)
        );
    }

    private Timer getOrCreateDurationTimer(String status) {
        return durationTimers.computeIfAbsent(status, k ->
                Timer.builder(METRIC_PREFIX + "_duration_seconds")
                        .description("Time taken to evaluate request context")
                        .tag(TAG_STATUS, status)
                        .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(registry)
        );
    }

    private Timer getOrCreateActiveDurationTimer(String scoreLevel, String decision) {
        String key = scoreLevel + "_" + decision;
        return activeDurationTimers.computeIfAbsent(key, k ->
                Timer.builder(METRIC_PREFIX + "_active_duration_seconds")
                        .description("Time taken to evaluate ACTIVE profile requests by score level")
                        .tag(TAG_SCORE_LEVEL, scoreLevel)
                        .tag(TAG_DECISION, decision)
                        .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(registry)
        );
    }

    // ==================== Convenience methods for specific scenarios ====================

    /**
     * Records a BLOCKED status evaluation (immediate denial).
     */
    public void recordBlockedStatus(long durationMs) {
        recordEvaluation(AdminProfileStatus.BLOCKED, Decision.BLOCKED, 1.0, durationMs, 0.7);
    }

    /**
     * Records a LEARNING status evaluation (always allowed).
     */
    public void recordLearningEvaluation(double score, long durationMs) {
        recordEvaluation(AdminProfileStatus.LEARNING, Decision.ALLOWED, score, durationMs, 0.7);
    }
}
