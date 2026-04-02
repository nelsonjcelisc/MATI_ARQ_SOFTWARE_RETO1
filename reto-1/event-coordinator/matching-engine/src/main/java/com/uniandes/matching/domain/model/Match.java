package com.uniandes.matching.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    private String id;
    private String buyOrderId;
    private String saleOrderId;
    private String symbol;
    private Integer quantity;
    private BigDecimal price;
    private LocalDateTime timestamp;

    public void initialize() {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public static Match create(Order buyOrder, Order saleOrder, Integer quantity) {
        Match match = Match.builder()
                .buyOrderId(buyOrder.getId())
                .saleOrderId(saleOrder.getId())
                .symbol(buyOrder.getSymbol())
                .quantity(quantity)
                .price(saleOrder.getPrice())
                .build();

        match.initialize();
        return match;
    }
}
