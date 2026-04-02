package com.uniandes.matching.domain.repository;

import com.uniandes.matching.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface SaleOrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findBySymbol(String symbol);

    List<Order> findAll();

    void deleteById(String id);
}
