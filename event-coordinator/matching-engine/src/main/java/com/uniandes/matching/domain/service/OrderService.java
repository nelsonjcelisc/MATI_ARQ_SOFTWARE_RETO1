package com.uniandes.matching.domain.service;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.model.OrderType;
import com.uniandes.matching.domain.repository.BuyOrderRepository;
import com.uniandes.matching.domain.repository.SaleOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService {
    private final SaleOrderRepository saleOrderRepository;
    private final BuyOrderRepository buyOrderRepository;

    public OrderService(SaleOrderRepository saleOrderRepository,
                        BuyOrderRepository buyOrderRepository) {
        this.saleOrderRepository = saleOrderRepository;
        this.buyOrderRepository = buyOrderRepository;
    }

    public Order createOrder(Order order) {
        order.initialize();

        // ⚠️ SOLO PARA TESTING - Simula BD lenta
//        try {
//            Thread.sleep(200); // 200ms delay
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
        // ⚠️ FIN TESTING

        log.info("Creating order: {} - Type: {} - Symbol: {}", order.getId(), order.getType(), order.getSymbol());

        if (order.getType() == OrderType.SALE) {
            return saleOrderRepository.save(order);
        } else {
            return buyOrderRepository.save(order);
        }
    }
}
