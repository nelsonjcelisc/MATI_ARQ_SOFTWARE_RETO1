package com.uniandes.ventas.web.controller;

import com.uniandes.ventas.domain.service.OrdenCompraService;
import com.uniandes.ventas.web.dto.OrdenCompraRequest;
import com.uniandes.ventas.web.dto.OrdenCompraResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/api/orden_compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crearOrdenCompra(@Valid @RequestBody OrdenCompraRequest request,
                                                                @RequestHeader("Idempotencia-Key") String idempotencyKey) {

        OrdenCompraResponse response = ordenCompraService.crearOrdenCompra(request, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Idempotencia-Key", idempotencyKey)
                    .body(response);
    }
}
