package com.uniandes.matching.web.controller;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.events.bus.EventBus;
import com.uniandes.matching.events.model.EventType;
import com.uniandes.matching.events.model.OrderEvent;
import com.uniandes.matching.web.dto.OrderRequest;
import com.uniandes.matching.web.dto.OrderResponse;
import com.uniandes.matching.web.mapper.OrderDtoMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final EventBus eventBus;
    private final OrderDtoMapper mapper;

    public OrderController(EventBus eventBus, OrderDtoMapper mapper) {
        this.eventBus = eventBus;
        this.mapper = mapper;
    }

    @PostMapping("/sale")
    public ResponseEntity<OrderResponse> createSaleOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received SALE order request: Symbol={}, Qty={}, Price={}",
                request.getSymbol(), request.getQuantity(), request.getPrice());

        Order order = mapper.toDomain(request);
        order.initialize();

        OrderEvent event = OrderEvent.of(EventType.SALE_RECEIVED, order);
        eventBus.publish(event);

        log.info("SALE order published to event bus: {}", order.getId());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(OrderResponse.accepted(order.getId()));
    }

    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> createBuyOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received BUY order request: Symbol={}, Qty={}, Price={}",
                request.getSymbol(), request.getQuantity(), request.getPrice());

        Order order = mapper.toDomain(request);
        order.initialize();

        OrderEvent event = OrderEvent.of(EventType.BUY_RECEIVED, order);
        eventBus.publish(event);

        log.info("BUY order published to event bus: {}", order.getId());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(OrderResponse.accepted(order.getId()));
    }
}