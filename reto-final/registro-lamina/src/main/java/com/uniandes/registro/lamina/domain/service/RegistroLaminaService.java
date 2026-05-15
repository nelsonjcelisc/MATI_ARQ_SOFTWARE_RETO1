package com.uniandes.registro.lamina.domain.service;

import com.uniandes.registro.lamina.domain.model.RegistroLaminaDTO;
import com.uniandes.registro.lamina.domain.util.ConstantsVentas;
import com.uniandes.registro.lamina.persistence.impl.RegistroLaminaRepositoryImpl;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaRequest;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaResponse;
import com.uniandes.registro.lamina.web.mapper.RegistroLaminaMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistroLaminaService {
    private static final Logger log = LoggerFactory.getLogger(RegistroLaminaService.class);

    // Logger específico para duplicados
    private static final Logger logDuplicados = LoggerFactory.getLogger("LoggerDuplicados");

    private final RegistroLaminaRepositoryImpl registroLaminaRepositoryImpl;
    private final RegistroLaminaMapper registroLaminaMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    public RegistroLaminaResponse registrarLamina(RegistroLaminaRequest request, String idempotencyKey, HttpServletRequest requestController) {

        String redisKey = ConstantsVentas.REDIS_PREFIX + idempotencyKey;
       // Timer.Sample sample = Timer.start(registry);
        Timer.Sample sample = (Timer.Sample) requestController.getAttribute("IDEMPOTENCY_TIMER");
        System.out.println("Timer start: " +sample);
        // Intento de adquirir el lock
       /* Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(
                redisKey + ":lock",
                "PROCESSING",
                Duration.ofSeconds(10)
        );

        boolean lockOwner = Boolean.TRUE.equals(isNewRequest);

        if (!lockOwner) {
            sample.stop(registry.timer("orden.tiempo.total"));
            return handleDuplicate(redisKey, idempotencyKey);
        }*/

            return ejecutarPersistencia(request, idempotencyKey, redisKey, sample);

    }


    @Transactional
    public RegistroLaminaResponse ejecutarPersistencia(RegistroLaminaRequest request, String idempotencyKey, String redisKey, Timer.Sample sample) {

        try {
            request.setFechaRegistro(LocalDateTime.now());

            RegistroLaminaDTO dto = registroLaminaMapper.fromRegistroLaminaRequest(request);
            dto = registroLaminaRepositoryImpl.save(dto);
            RegistroLaminaResponse response = registroLaminaMapper.fromDTO(dto);

            saveInCache(redisKey, response);
            return response;

        } catch (DataIntegrityViolationException e) {
            long tiempoNano = sample.stop(registry.timer("registro.tiempo.total"));
            long tiempoMs = TimeUnit.NANOSECONDS.toMillis(tiempoNano);
            System.out.println("Tiempo Fin ms: " + tiempoMs);
            logDuplicados.info( "Duplicado detectado en BD. ID LAMINA={}", idempotencyKey );
            throw new ResponseStatusException( HttpStatus.CONFLICT, "La lámina " + idempotencyKey +" ya fue registrada ..." );
        }
    }

    private RegistroLaminaResponse handleDuplicate(String redisKey, String idempotencyKey) {
        logDuplicados.info( "Duplicado detectado en BD. ID LAMINA={}", idempotencyKey );
        throw new ResponseStatusException( HttpStatus.CONFLICT, "La lámina " + idempotencyKey +" ya fue registrada ..." );
    }

    private void saveInCache(String key, RegistroLaminaResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(15));
        } catch (Exception e) {
            log.error("No se pudo guardar en Redis: {}", e.getMessage());
        }
    }
}
