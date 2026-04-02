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
public class Order {

    private String id;
    private String symbol;
    private Integer quantity;
    private BigDecimal price;
    private OrderType type;
    private String userId;
    private LocalDateTime timestamp;

    public void initialize() {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public boolean canMatchWith(Order other) {
        if (this.type == other.type) {
            return false;
        }

        if (!this.symbol.equals(other.symbol)) {
            return false;
        }

        if (this.type == OrderType.BUY) {
            return this.price.compareTo(other.price) >= 0;
        } else {
            return other.price.compareTo(this.price) >= 0;
        }
    }
}
