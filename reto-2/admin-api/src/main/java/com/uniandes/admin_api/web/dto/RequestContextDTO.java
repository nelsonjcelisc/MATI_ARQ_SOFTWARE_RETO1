package com.uniandes.admin_api.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Captures the context of an incoming request for anomaly scoring.
 *
 * This DTO holds all the contextual information extracted from request headers
 * that will be evaluated against the admin's trusted profile.
 */
@Data
@Builder
public class RequestContextDTO {

    /**
     * Admin identifier extracted from X-User-Id header (propagated by Kong).
     */
    private String adminId;

    /**
     * Device fingerprint extracted from X-Device-Id header.
     */
    private String device;

    /**
     * Client IP address extracted from X-Forwarded-For header.
     */
    private String ip;

    /**
     * User-Agent string from the request header.
     */
    private String userAgent;

    /**
     * Hour of the request (0-23) for time-based anomaly detection.
     */
    private Integer hour;

    /**
     * Timestamp when the request was received.
     */
    private LocalDateTime timestamp;

    /**
     * The request URI being accessed.
     */
    private String requestUri;

    /**
     * HTTP method of the request.
     */
    private String method;
}
