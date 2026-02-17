package com.uniandes.matching.domain.service;

import com.uniandes.matching.domain.model.Match;
import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.model.OrderType;
import com.uniandes.matching.domain.repository.BuyOrderRepository;
import com.uniandes.matching.domain.repository.SaleOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OrderService {
    private final SaleOrderRepository saleOrderRepository;
    private final BuyOrderRepository buyOrderRepository;
    private final NotificationService notificationService;
    private final MatchingServiceClient matchingServiceClient;

    public OrderService(SaleOrderRepository saleOrderRepository,
                        BuyOrderRepository buyOrderRepository,
                        NotificationService notificationService,
                        MatchingServiceClient matchingServiceClient) {
        this.saleOrderRepository = saleOrderRepository;
        this.buyOrderRepository = buyOrderRepository;
        this.notificationService = notificationService;
        this.matchingServiceClient = matchingServiceClient;
    }

    public Order createOrder(Order order) {
        order.initialize();

        // SOLO PARA TESTING - Simula BD lenta
//        try {
//            Thread.sleep(200); // 200ms delay
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
        // FIN TESTING

        log.info("Creating order: {} - Type: {} - Symbol: {}", order.getId(), order.getType(), order.getSymbol());

        Order savedOrder;
        if (order.getType() == OrderType.SALE) {
            savedOrder = saleOrderRepository.save(order);
        } else {
            savedOrder = buyOrderRepository.save(order);
        }

        // Llamar al servicio real de matching de Node.js
        try {
            List<Match> matches = matchingServiceClient.performMatching("matching-engine-java");
            
            // Notificar cada match encontrado
            for (Match match : matches) {
                log.info("Match encontrado: {} - Symbol: {} - Quantity: {} - Price: {}", 
                        match.getId(), match.getSymbol(), match.getQuantity(), match.getPrice());
                notificationService.sendMatchNotification(match);
            }
            
            if (matches.isEmpty()) {
                log.info("No se encontraron matches para la orden: {}", savedOrder.getId());
            }
        } catch (Exception e) {
            log.error("Error al realizar matching para la orden: {}", savedOrder.getId(), e);
            // Continuar sin lanzar excepción para no interrumpir el flujo
        }

        return savedOrder;
    }
}
