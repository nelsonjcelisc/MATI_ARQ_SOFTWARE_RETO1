package com.uniandes.admin_api.web.filter;

import com.uniandes.admin_api.domain.model.Decision;
import com.uniandes.admin_api.domain.service.ContextScoringService;
import com.uniandes.admin_api.domain.service.ContextScoringService.ScoringResult;
import com.uniandes.admin_api.infrastructure.config.ContextScorerProperties;
import com.uniandes.admin_api.infrastructure.metrics.ContextScorerMetrics;
import com.uniandes.admin_api.web.dto.RequestContextDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Context Scorer Filter - Layer 2 of Defense in Depth for Security.
 *
 * This filter intercepts all incoming requests after Kong's Layer 1 (JWT + ACL)
 * and evaluates the request context against the admin's trusted behavioral profile.
 *
 * Flow:
 * 1. Extract admin ID from X-User-Id header (propagated by Kong)
 * 2. Extract context: device fingerprint, IP, User-Agent, request hour
 * 3. Query Redis profile store for trusted values
 * 4. Calculate anomaly score using weighted features
 * 5. If score >= threshold: Return 403 + publish event async
 * 6. If score < threshold: Allow request through to controller
 *
 * Bass Tactics: Identify Actors + Detect Intrusions
 *
 * Sensitivity Points:
 * - Number of features evaluated
 * - Score threshold (configurable)
 * - Redis query latency
 * - Async event publishing (does not block 403 response)
 */
@Slf4j
@Component
@Order(1) // Execute early in filter chain
@RequiredArgsConstructor
public class ContextScorerFilter extends OncePerRequestFilter {

    private final ContextScoringService scoringService;
    private final ContextScorerProperties properties;
    private final ContextScorerMetrics metrics;

    /**
     * Paths that should be excluded from context scoring.
     * - Health checks, metrics, actuator endpoints
     * - Ping endpoint for basic connectivity
     */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator",
            "/health",
            "/metrics",
            "/prometheus",
            "/vendor/ping"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip if filter is disabled
        if (!properties.isEnabled()) {
            metrics.recordSkipped("disabled");
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());

        // Skip excluded paths
        if (shouldSkipPath(path)) {
            metrics.recordSkipped("excluded_path");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract admin ID from Kong-propagated header
        String adminId = request.getHeader(properties.getHeaders().getUserId());

        // Defense in Depth: Block requests without X-User-Id header
        // This should have been validated by Kong (Layer 1), but we enforce it here as Layer 2
        if (adminId == null || adminId.isBlank()) {
            log.warn("UNAUTHORIZED - Missing X-User-Id header for path: {}. Request blocked.", path);
            metrics.recordMissingHeader();
            sendUnauthorizedResponse(response, "Missing required X-User-Id header");
            return;
        }

        // Build request context
        RequestContextDTO context = buildRequestContext(request, adminId);

        // Log entry for timing measurement (t0 per spec)
        long startTime = System.currentTimeMillis();
        log.info("Context Scorer evaluating request - adminId: {}, path: {}, method: {}",
                adminId, path, request.getMethod());

        // Evaluate context and get scoring result
        ScoringResult result = scoringService.evaluateContext(context);

        // Calculate evaluation duration
        long durationMs = System.currentTimeMillis() - startTime;

        // Record metrics for this evaluation
        metrics.recordEvaluation(
                result.getProfileStatus(),
                result.getDecision(),
                result.getScore(),
                durationMs,
                properties.getThreshold()
        );

        // Record profile not found if applicable
        if (result.getProfileStatus() == null && "Profile not found".equals(result.getReason())) {
            metrics.recordProfileNotFound();
        }

        // Persist event asynchronously (does not block response)
        scoringService.persistEventAsync(context, result);

        // Check decision
        if (result.getDecision() == Decision.BLOCKED) {
            log.warn("BLOCKED - Admin: {}, Score: {:.2f}, Reason: {}, ResponseTime: {}ms",
                    adminId, result.getScore(), result.getReason(), durationMs);

            sendForbiddenResponse(response, result);
            return;
        }

        // Allow request through
        log.info("ALLOWED - Admin: {}, Score: {:.2f}, ResponseTime: {}ms",
                adminId, result.getScore(), durationMs);

        filterChain.doFilter(request, response);
    }

    /**
     * Builds the request context DTO from the HTTP request.
     */
    private RequestContextDTO buildRequestContext(HttpServletRequest request, String adminId) {
        ContextScorerProperties.Headers headers = properties.getHeaders();

        // Extract device ID
        String device = request.getHeader(headers.getDeviceId());

        // Extract IP - prefer X-Forwarded-For, fallback to remote addr
        String ip = request.getHeader(headers.getForwardedFor());
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first (original client)
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }

        // Extract User-Agent
        String userAgent = request.getHeader("User-Agent");

        // Get current hour
        int hour = LocalDateTime.now().getHour();

        return RequestContextDTO.builder()
                .adminId(adminId)
                .device(device)
                .ip(ip)
                .userAgent(userAgent)
                .hour(hour)
                .timestamp(LocalDateTime.now())
                .requestUri(request.getRequestURI())
                .method(request.getMethod())
                .build();
    }

    /**
     * Sends a 401 Unauthorized response when required headers are missing.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"status\":%d,\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                HttpStatus.UNAUTHORIZED.value(),
                escapeJson(reason),
                LocalDateTime.now().toString()
        );

        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
    }

    /**
     * Sends a 403 Forbidden response with details about the block reason.
     */
    private void sendForbiddenResponse(HttpServletResponse response, ScoringResult result) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Build JSON response manually to avoid ObjectMapper dependency
        String jsonResponse = String.format(
                "{\"status\":%d,\"error\":\"Forbidden\",\"message\":\"Access denied due to anomalous context\",\"timestamp\":\"%s\",\"score\":%.2f,\"reason\":\"%s\"}",
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now().toString(),
                result.getScore(),
                escapeJson(result.getReason())
        );

        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
    }

    /**
     * Escapes special characters in JSON string values.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Determines if a path should be excluded from context scoring.
     */
    private boolean shouldSkipPath(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded) || path.equals(excluded)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS requests (CORS preflight) should not be filtered
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
