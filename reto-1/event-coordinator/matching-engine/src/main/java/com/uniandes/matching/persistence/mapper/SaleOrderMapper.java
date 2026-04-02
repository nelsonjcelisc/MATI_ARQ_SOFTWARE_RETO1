package com.uniandes.matching.persistence.mapper;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.model.OrderType;
import com.uniandes.matching.persistence.entity.SaleOrderEntity;
import org.springframework.stereotype.Component;

@Component
public class SaleOrderMapper {

    public SaleOrderEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }

        return SaleOrderEntity.builder()
                .id(order.getId())
                .symbol(order.getSymbol())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .userId(order.getUserId())
                .timestamp(order.getTimestamp())
                .build();
    }

    public Order toDomain(SaleOrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return Order.builder()
                .id(entity.getId())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .type(OrderType.SALE)
                .userId(entity.getUserId())
                .timestamp(entity.getTimestamp())
                .build();
    }
}