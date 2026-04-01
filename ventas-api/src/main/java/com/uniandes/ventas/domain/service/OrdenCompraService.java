package com.uniandes.ventas.domain.service;


import com.uniandes.ventas.domain.model.OrdenCompraDTO;
import com.uniandes.ventas.domain.model.OrdenCompraStatus;
import com.uniandes.ventas.domain.repository.OrdenCompraRepository;
import com.uniandes.ventas.domain.util.ConstantsVentas;
import com.uniandes.ventas.persistence.impl.OrdenCompraRepositoryImpl;
import com.uniandes.ventas.web.dto.OrdenCompraRequest;
import com.uniandes.ventas.web.dto.OrdenCompraResponse;
import com.uniandes.ventas.web.mapper.OrdenCompraMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private static final Logger log = LoggerFactory.getLogger(OrdenCompraService.class);

    // Logger específico para duplicados
    private static final Logger logDuplicados = LoggerFactory.getLogger("LoggerDuplicados");

    private final Timer ordenTimer;
    private final OrdenCompraRepositoryImpl ordenCompraRepositoryImpl;
    private final OrdenCompraMapper ordenCompraMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrdenCompraResponse crearOrdenCompra(OrdenCompraRequest request, String idempotencyKey, Timer.Sample sample) {

        // 1. DEFINIR LA LLAVE DE REDIS
        String redisKey = ConstantsVentas.REDIS_PREFIX + idempotencyKey;
        request.setEstado(OrdenCompraStatus.PAGADA);
        request.setFechaCompra(LocalDateTime.now());

        // 2. INTENTAR RECUPERAR DE REDIS (Caché rápida)
        String cachedJson = redisTemplate.opsForValue().get(redisKey);
        if (cachedJson != null) {
            try {
                //Detenemos el timer para la medicion de deteccion en Prometheus al detectarlo en redis
                sample.stop(ordenTimer);
                // Convertimos el JSON de Redis directamente al Response que espera el Controller
                logDuplicados.info("Intento de operación duplicada por UUID detectado. UUID: {}, idFactura: {}", idempotencyKey, request.getIdFactura());

                return objectMapper.readValue(cachedJson, OrdenCompraResponse.class);
            } catch (Exception e) {
                log.error("Error al deserializar desde Redis: {}", e.getMessage());
                // Si falla la caché, no detenemos el flujo, dejamos que siga a la DB
            }
        }

        // 3. VERIFICAR  idFactura EN BASE DE DATOS (Por si UUID en la caché expiró o no existe)
        var existente = ordenCompraRepositoryImpl.findByIdFactura(request.getIdFactura());

        if (existente.isPresent()) {
            //Detenemos el timer para la medicion de deteccion en Prometheus al detectarlo en la base de datos
            sample.stop(ordenTimer);
            logDuplicados.info("Intento de operación duplicada por idFactura detectado. UUID {},  idFactura={}", idempotencyKey, request.getIdFactura());
            OrdenCompraResponse response = ordenCompraMapper.fromDTO(existente.get());

            //Guardamos en session el idFactura duplicado para leerlo en el Exception Handler
            RequestContextHolder.getRequestAttributes()
                    .setAttribute("ordenCompraResponse", response, RequestAttributes.SCOPE_REQUEST);

            return response;
        }

        // 4. PROCESAR NUEVA ORDEN (Si no existe en ningún lado)

        // Mapeo de entrada: Request -> DTO
        OrdenCompraDTO ordenCompraDTO = ordenCompraMapper.fromOrderCompraRequest(request);

        // Lógica y Persistencia
        ordenCompraDTO = ordenCompraRepositoryImpl.save(ordenCompraDTO);

        // Mapeo de salida: DTO -> Response
        OrdenCompraResponse response = ordenCompraMapper.fromDTO(ordenCompraDTO);

        // 5. GUARDAR EN CACHÉ (Para que la siguiente petición sea instantánea)
        saveInCache(redisKey, response);

        return response;

    }

    /**
     * Método auxiliar para no ensuciar la lógica principal con Try/Catch
     */
    private void saveInCache(String key, OrdenCompraResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            // Guardamos por 15 minutos (ajustable)
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(15));
        } catch (Exception e) {
            log.error("No se pudo guardar en Redis: {}", e.getMessage());
        }
    }

}
