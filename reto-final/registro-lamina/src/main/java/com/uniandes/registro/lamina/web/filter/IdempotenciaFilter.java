package com.uniandes.registro.lamina.web.filter;

import com.uniandes.registro.lamina.domain.util.ConstantsVentas;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class IdempotenciaFilter extends OncePerRequestFilter {

    private static final Logger logDuplicados = LoggerFactory.getLogger("LoggerDuplicados");

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry registry;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.contains("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Timer.Sample sample = Timer.start(registry);
        String key = request.getHeader("Idempotencia-Key");
        String redisKey = ConstantsVentas.REDIS_PREFIX + key;
        request.setAttribute("IDEMPOTENCY_TIMER", sample);

        // Intento de adquirir el lock
        Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(
                redisKey + ":lock",
                "PROCESSING",
                Duration.ofSeconds(10)
        );

        boolean lockOwner = Boolean.TRUE.equals(isNewRequest);

        try {
            //Validación de existencia Idempotencia-Key
            if (key == null || key.isBlank()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de 'Idempotency-Key' inválido, no puede ser null.");
                return;
            }

            if (!lockOwner) {
                sample.stop(registry.timer("registro.tiempo.total"));
                logDuplicados.info( "Duplicado detectado en BD. ID LAMINA={}", key );
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Filter: La lámina " + key + " ya fue registrada ...");
                return;
            }


        filterChain.doFilter(request, response);

        } finally {
            if (lockOwner) {
               redisTemplate.delete(redisKey + ":lock");
            }
        }
    }

    // Método auxiliar para responder con JSON
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}