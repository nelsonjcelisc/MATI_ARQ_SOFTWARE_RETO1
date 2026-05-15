package com.uniandes.registro.lamina.domain.exception;

import com.uniandes.registro.lamina.persistence.impl.RegistroLaminaRepositoryImpl;
import com.uniandes.registro.lamina.web.dto.ErrorResponse;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaResponse;
import com.uniandes.registro.lamina.web.mapper.RegistroLaminaMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
public class RegistroLaminaExceptionHandler {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RegistroLaminaRepositoryImpl registroLaminaRepositoryImpl;
    private final RegistroLaminaMapper ordenCompraMapper;

    private static final Logger log = LoggerFactory.getLogger(RegistroLaminaExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_PARAMETER",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /*@ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        // Usamos CONFLICT (409) que es más preciso para violaciones de unicidad que BAD_REQUEST (400)
        ErrorResponse error = new ErrorResponse(
                "DATA_INTEGRITY_ERROR",
                ex.getMessage() + " \n El registro ya existe o viola una restricción de integridad.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }*/

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex) {

        log.info("Exception Detection DataIntegrityViolationException");

        //Leemos de la session  registroLaminaResponse duplicado para buscarlo y enviarlo a la respeusta
        RegistroLaminaResponse registroLaminaResponse =
                (RegistroLaminaResponse) RequestContextHolder.getRequestAttributes()
                        .getAttribute("ordenCompraResponse", RequestAttributes.SCOPE_REQUEST);

        //Handler Exception
        if (registroLaminaResponse != null ) {
            log.info("Handler Exception: Reparando flujo desde la session");
            return ResponseEntity.ok(registroLaminaResponse);
        }

        //Si no hay objeto en la session, devolvemos el error original
        ErrorResponse error = new ErrorResponse(
                "DATA_INTEGRITY_ERROR",
                "Error de integridad, la transacción ya existe pero no se pudo recuperar el detalle.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
