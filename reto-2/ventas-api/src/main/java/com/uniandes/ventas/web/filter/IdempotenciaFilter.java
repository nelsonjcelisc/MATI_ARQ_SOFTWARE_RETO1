package com.uniandes.ventas.web.filter;

import com.uniandes.ventas.domain.util.ConstantsVentas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class IdempotenciaFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.contains("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = request.getHeader("Idempotencia-Key");

        //Validación de existencia Idempotencia-Key
        if (key == null || key.isBlank()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de 'Idempotency-Key' inválido. Se espera un UUID.");
            return;
        }

        //VALIDACIÓN DE FORMATO (UUID)
        if (!key.matches(ConstantsVentas.UUID_PATTERN)) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de 'Idempotency-Key' inválido. Se espera un UUID.");
            return;
        }
        filterChain.doFilter(request, response);
    }
    // Método auxiliar para responder con JSON
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}