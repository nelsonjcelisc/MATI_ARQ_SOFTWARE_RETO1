package com.uniandes.matching.web.mapper;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.web.dto.OrderRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderDtoMapper {

    public Order toDomain(OrderRequest request) {
        if (request == null) {
            return null;
        }

        return Order.builder()
                .symbol(request.getSymbol().toUpperCase())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .type(request.getType())
                .userId(request.getUserId())
                .build();
    }
}