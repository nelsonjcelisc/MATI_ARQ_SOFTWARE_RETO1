package com.uniandes.matching.persistence.mapper;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.model.OrderType;
import com.uniandes.matching.persistence.entity.BuyOrderEntity;
import org.springframework.stereotype.Component;

@Component
public class BuyOrderMapper {

    public BuyOrderEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }

        return BuyOrderEntity.builder()
                .id(order.getId())
                .symbol(order.getSymbol())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .userId(order.getUserId())
                .timestamp(order.getTimestamp())
                .build();
    }

    public Order toDomain(BuyOrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return Order.builder()
                .id(entity.getId())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .type(OrderType.BUY)
                .userId(entity.getUserId())
                .timestamp(entity.getTimestamp())
                .build();
    }
}