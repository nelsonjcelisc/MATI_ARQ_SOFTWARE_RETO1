package com.uniandes.ventas.domain.service;

import com.uniandes.ventas.domain.model.OrdenCompraDTO;
import com.uniandes.ventas.domain.model.OrdenCompraStatus;
import com.uniandes.ventas.domain.util.ConstantsVentas;
import com.uniandes.ventas.persistence.impl.OrdenCompraRepositoryImpl;
import com.uniandes.ventas.web.dto.OrdenCompraRequest;
import com.uniandes.ventas.web.dto.OrdenCompraResponse;
import com.uniandes.ventas.web.mapper.OrdenCompraMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {
    private static final Logger log = LoggerFactory.getLogger(OrdenCompraService.class);

    // Logger específico para duplicados
    private static final Logger logDuplicados = LoggerFactory.getLogger("LoggerDuplicados");

    private final OrdenCompraRepositoryImpl ordenCompraRepositoryImpl;
    private final OrdenCompraMapper ordenCompraMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    public OrdenCompraResponse crearOrdenCompra( OrdenCompraRequest request, String idempotencyKey) {

        String redisKey = ConstantsVentas.REDIS_PREFIX + idempotencyKey;
        Timer.Sample sample = Timer.start(registry);
        String finalStatus = "error_inesperado";
        boolean lockOwner = false;

        try {
            // Intento de adquirir el lock
            Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(
                    redisKey + ":lock",
                    "PROCESSING",
                    Duration.ofSeconds(10)
            );

            lockOwner = Boolean.TRUE.equals(isNewRequest);

            if (!lockOwner) {
                finalStatus = "duplicate_redis";
                return handleDuplicate(redisKey, idempotencyKey, request, finalStatus);
            }

            return ejecutarPersistencia(request, idempotencyKey, redisKey, sample);

        } catch (DataIntegrityViolationException e) {
            finalStatus = "duplicate_db";
            logDuplicados.info( "Duplicado detectado en BD. UUID={}, idFactura={}", idempotencyKey, request.getIdFactura() );
            throw new ResponseStatusException( HttpStatus.ACCEPTED, "La petición está siendo procesada db..." );
        } finally {
            sample.stop(registry.timer("orden.tiempo.total", "status", finalStatus));
            if (lockOwner) {
                redisTemplate.delete(redisKey + ":lock");
            }
        }
    }


    @Transactional
    public OrdenCompraResponse ejecutarPersistencia( OrdenCompraRequest request, String idempotencyKey, String redisKey, Timer.Sample sample) {
            request.setEstado(OrdenCompraStatus.PAGADA);
            request.setFechaCompra(LocalDateTime.now());

            OrdenCompraDTO dto = ordenCompraMapper.fromOrderCompraRequest(request);
            dto = ordenCompraRepositoryImpl.save(dto);
            sample.stop(registry.timer("orden.tiempo.total", "status", "success"));
            OrdenCompraResponse response = ordenCompraMapper.fromDTO(dto);
            saveInCache(redisKey, response);
            return response;
    }

    private OrdenCompraResponse handleDuplicate(String redisKey, String idempotencyKey, OrdenCompraRequest request, String finalStatus) {
        logDuplicados.info( "Duplicado detectado en BD. UUID={}, idFactura={}", idempotencyKey, request.getIdFactura() );
        throw new ResponseStatusException(HttpStatus.ACCEPTED, "La petición está siendo procesada redis... " + finalStatus);
    }

    private void saveInCache(String key, OrdenCompraResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(15));
        } catch (Exception e) {
            log.error("No se pudo guardar en Redis: {}", e.getMessage());
        }
    }
}
