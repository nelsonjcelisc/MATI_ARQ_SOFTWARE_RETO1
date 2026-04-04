package com.uniandes.admin_api.domain.service;

import com.uniandes.admin_api.domain.model.AdminContextEventDTO;
import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import com.uniandes.admin_api.domain.model.Decision;
import com.uniandes.admin_api.domain.repository.AdminContextEventRepository;
import com.uniandes.admin_api.infrastructure.config.ContextScorerProperties;
import com.uniandes.admin_api.infrastructure.redis.RedisProfileStore;
import com.uniandes.admin_api.web.dto.RequestContextDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Service responsible for calculating anomaly scores for admin requests.
 *
 * The scoring algorithm evaluates the request context against the admin's
 * trusted profile stored in Redis. Each context feature (device, IP, hour,
 * user-agent) contributes to the total score based on configurable weights.
 *
 * Score interpretation:
 * - 0.0: All context features match trusted profile (no anomaly)
 * - 1.0: No context features match (maximum anomaly)
 *
 * Bass Tactic: Detect Intrusions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextScoringService {

    private final RedisProfileStore redisProfileStore;
    private final ContextScorerProperties properties;
    private final AdminContextEventRepository contextEventRepository;

    /**
     * Evaluates the request context and returns a scoring result.
     *
     * @param context The request context to evaluate
     * @return ScoringResult containing the score and decision
     */
    public ScoringResult evaluateContext(RequestContextDTO context) {
        String adminId = context.getAdminId();

        // Check if profile exists in Redis
        if (!redisProfileStore.profileExists(adminId)) {
            log.warn("No profile found in Redis for adminId: {}. Blocking request.", adminId);
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(1.0)
                    .decision(Decision.BLOCKED)
                    .reason("Profile not found")
                    .build();
        }

        // Check profile status
        Optional<AdminProfileStatus> statusOpt = redisProfileStore.getStatus(adminId);
        if (statusOpt.isEmpty()) {
            log.warn("Could not retrieve status for adminId: {}. Blocking request.", adminId);
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(1.0)
                    .decision(Decision.BLOCKED)
                    .reason("Status unavailable")
                    .build();
        }

        AdminProfileStatus status = statusOpt.get();

        // Block if profile is BLOCKED
        if (status == AdminProfileStatus.BLOCKED) {
            log.info("Admin {} has BLOCKED status. Denying access.", adminId);
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(1.0)
                    .decision(Decision.BLOCKED)
                    .reason("Admin profile is blocked")
                    .build();
        }

        // Calculate anomaly score
        double score = calculateScore(context);
        double threshold = properties.getThreshold();

        // For LEARNING profiles, we allow but still log
        if (status == AdminProfileStatus.LEARNING) {
            log.info("Admin {} is in LEARNING mode. Score: {}. Allowing request.", adminId, score);
            return ScoringResult.builder()
                    .adminId(adminId)
                    .score(score)
                    .decision(Decision.ALLOWED)
                    .reason("Learning mode - data collection")
                    .build();
        }

        // For ACTIVE profiles, apply threshold
        Decision decision = score >= threshold ? Decision.BLOCKED : Decision.ALLOWED;
        String reason = decision == Decision.BLOCKED
                ? String.format("Anomaly score %.2f exceeds threshold %.2f", score, threshold)
                : "Context within normal parameters";

        log.info("Admin {} score: {} (threshold: {}). Decision: {}",
                adminId, String.format("%.2f", score), threshold, decision);

        return ScoringResult.builder()
                .adminId(adminId)
                .score(score)
                .decision(decision)
                .reason(reason)
                .deviceMatch(!isDeviceAnomalous(context))
                .ipMatch(!isIpAnomalous(context))
                .hourMatch(!isHourAnomalous(context))
                .userAgentMatch(!isUserAgentAnomalous(context))
                .build();
    }

    /**
     * Calculates the anomaly score based on weighted feature evaluation.
     *
     * Each feature that does NOT match the trusted profile contributes
     * its weight to the total score.
     */
    private double calculateScore(RequestContextDTO context) {
        ContextScorerProperties.Weights weights = properties.getWeights();
        double score = 0.0;

        // Device check
        if (isDeviceAnomalous(context)) {
            score += weights.getDevice();
            log.debug("Device '{}' not in trusted list for admin {}", context.getDevice(), context.getAdminId());
        }

        // IP check
        if (isIpAnomalous(context)) {
            score += weights.getIp();
            log.debug("IP '{}' not in trusted list for admin {}", context.getIp(), context.getAdminId());
        }

        // Hour check
        if (isHourAnomalous(context)) {
            score += weights.getHour();
            log.debug("Hour '{}' not in trusted hours for admin {}", context.getHour(), context.getAdminId());
        }

        // User-Agent check
        if (isUserAgentAnomalous(context)) {
            score += weights.getUserAgent();
            log.debug("User-Agent '{}' not in trusted list for admin {}", context.getUserAgent(), context.getAdminId());
        }

        return Math.min(score, 1.0); // Cap at 1.0
    }

    private boolean isDeviceAnomalous(RequestContextDTO context) {
        if (context.getDevice() == null || context.getDevice().isBlank()) {
            return true; // Missing device is considered anomalous
        }
        Set<String> trustedDevices = redisProfileStore.getDevices(context.getAdminId());
        return trustedDevices == null || !trustedDevices.contains(context.getDevice());
    }

    private boolean isIpAnomalous(RequestContextDTO context) {
        if (context.getIp() == null || context.getIp().isBlank()) {
            return true;
        }
        Set<String> trustedIps = redisProfileStore.getIps(context.getAdminId());
        if (trustedIps == null || trustedIps.isEmpty()) {
            return true;
        }

        // Check exact match first
        if (trustedIps.contains(context.getIp())) {
            return false;
        }

        // Check for CIDR/prefix matches (e.g., "192.168.1." matches "192.168.1.100")
        String requestIp = context.getIp();
        for (String trustedIp : trustedIps) {
            if (trustedIp.endsWith(".") && requestIp.startsWith(trustedIp)) {
                return false;
            }
            // Support /24 notation (simplified)
            if (trustedIp.contains("/24")) {
                String prefix = trustedIp.substring(0, trustedIp.lastIndexOf('.') + 1);
                if (requestIp.startsWith(prefix)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isHourAnomalous(RequestContextDTO context) {
        if (context.getHour() == null) {
            return true;
        }
        Set<String> trustedHours = redisProfileStore.getHours(context.getAdminId());
        return trustedHours == null || !trustedHours.contains(String.valueOf(context.getHour()));
    }

    private boolean isUserAgentAnomalous(RequestContextDTO context) {
        if (context.getUserAgent() == null || context.getUserAgent().isBlank()) {
            return true;
        }
        Set<String> trustedAgents = redisProfileStore.getAgents(context.getAdminId());
        if (trustedAgents == null || trustedAgents.isEmpty()) {
            return true;
        }

        // Check exact match first
        if (trustedAgents.contains(context.getUserAgent())) {
            return false;
        }

        // Check partial match (User-Agents can vary slightly)
        String requestAgent = context.getUserAgent().toLowerCase();
        for (String trustedAgent : trustedAgents) {
            if (requestAgent.contains(trustedAgent.toLowerCase()) ||
                trustedAgent.toLowerCase().contains(requestAgent)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Persists the context event to the database asynchronously.
     * This does not block the response to the client.
     */
    @Async
    public void persistEventAsync(RequestContextDTO context, ScoringResult result) {
        try {
            AdminContextEventDTO eventDTO = AdminContextEventDTO.builder()
                    .adminId(context.getAdminId())
                    .device(context.getDevice())
                    .ip(context.getIp())
                    .hour(context.getHour())
                    .agent(context.getUserAgent())
                    .score(result.getScore())
                    .decision(result.getDecision())
                    .timestamp(LocalDateTime.now())
                    .build();

            contextEventRepository.save(eventDTO);
            log.debug("Persisted context event for admin {}: {}", context.getAdminId(), result.getDecision());
        } catch (Exception e) {
            log.error("Failed to persist context event for admin {}: {}", context.getAdminId(), e.getMessage());
        }
    }

    /**
     * Result of the scoring evaluation.
     */
    @Data
    @Builder
    public static class ScoringResult {
        private String adminId;
        private double score;
        private Decision decision;
        private String reason;

        // Individual feature match results (for debugging/logging)
        @Builder.Default
        private boolean deviceMatch = false;
        @Builder.Default
        private boolean ipMatch = false;
        @Builder.Default
        private boolean hourMatch = false;
        @Builder.Default
        private boolean userAgentMatch = false;
    }
}
