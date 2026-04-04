package com.uniandes.admin_api.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Context Scorer Filter.
 *
 * These properties control the anomaly detection behavior:
 * - threshold: Score above which a request is considered anomalous (0.0 - 1.0)
 * - weights: Relative importance of each context feature in the scoring algorithm
 * - enabled: Toggle to enable/disable the filter
 *
 * Registered via @EnableConfigurationProperties in AdminApiApplication.
 */
@Data
@ConfigurationProperties(prefix = "security.context-scorer")
public class ContextScorerProperties {

    /**
     * Enable or disable the context scorer filter.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Risk score threshold above which a request is blocked.
     * Optimal value per spec: 0.7
     * - Too low (< 0.5): High false positives
     * - Too high (> 0.9): Misses attacks
     */
    private double threshold = 0.7;

    /**
     * Weight configuration for each context feature.
     * Weights should sum to 1.0 for normalized scoring.
     */
    private Weights weights = new Weights();

    /**
     * Header names used by Kong to propagate user information.
     */
    private Headers headers = new Headers();

    @Data
    public static class Weights {
        private double device = 0.30;
        private double ip = 0.30;
        private double hour = 0.20;
        private double userAgent = 0.20;
    }

    @Data
    public static class Headers {
        private String userId = "X-User-Id";
        private String userRole = "X-User-Role";
        private String deviceId = "X-Device-Id";
        private String forwardedFor = "X-Forwarded-For";
    }
}
