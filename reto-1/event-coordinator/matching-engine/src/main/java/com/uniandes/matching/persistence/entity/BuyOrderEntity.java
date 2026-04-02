package com.uniandes.matching.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "buy_orders", indexes = {
        @Index(name = "idx_buy_symbol", columnList = "symbol"),
        @Index(name = "idx_buy_timestamp", columnList = "timestamp")
})
public class BuyOrderEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}