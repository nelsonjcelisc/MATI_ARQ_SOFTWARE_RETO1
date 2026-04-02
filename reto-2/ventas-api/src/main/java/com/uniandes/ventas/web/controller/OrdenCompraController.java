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

    @Qualifier("ordenTimer")
    private final Timer ordenTimer;
    private final MeterRegistry registry;
    private final OrdenCompraService ordenCompraService;

    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crearOrdenCompra(@Valid @RequestBody OrdenCompraRequest request,
                                                                @RequestHeader("Idempotencia-Key") String idempotencyKey) {

        Timer.Sample sample = Timer.start(registry);
        OrdenCompraResponse response = ordenCompraService.crearOrdenCompra(request, idempotencyKey, sample);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Idempotencia-Key", idempotencyKey)
                    .body(response);
    }
}
