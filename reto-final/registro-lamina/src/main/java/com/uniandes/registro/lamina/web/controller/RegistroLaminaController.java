package com.uniandes.registro.lamina.web.controller;

import com.uniandes.registro.lamina.domain.service.RegistroLaminaService;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaRequest;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registro_lamina")
@RequiredArgsConstructor
public class RegistroLaminaController {

    private final RegistroLaminaService registroLaminaService;

    @PostMapping
    public ResponseEntity<RegistroLaminaResponse> registrarLamina(@Valid @RequestBody RegistroLaminaRequest request,
                                                                  @RequestHeader("Idempotencia-Key") String idempotencyKey,
                                                                  HttpServletRequest requestController) {

        RegistroLaminaResponse response = registroLaminaService.registrarLamina(request, idempotencyKey, requestController);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Idempotencia-Key", idempotencyKey)
                    .body(response);
    }
}
