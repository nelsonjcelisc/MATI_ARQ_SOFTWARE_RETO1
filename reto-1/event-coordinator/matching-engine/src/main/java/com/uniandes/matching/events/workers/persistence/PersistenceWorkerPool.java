package com.uniandes.matching.events.workers.persistence;

import com.uniandes.matching.domain.model.OrderType;
import com.uniandes.matching.domain.service.OrderService;
import com.uniandes.matching.events.bus.EventBus;
import com.uniandes.matching.events.model.EventType;
import com.uniandes.matching.events.model.OrderEvent;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class PersistenceWorkerPool {

    private final EventBus eventBus;
    private final OrderService orderService;

    @Value("${workers.persistence.sales.count:8}")
    private int salesWorkerCount;

    @Value("${workers.persistence.buys.count:12}")
    private int buysWorkerCount;

    @Value("${workers.persistence.sales.queue-capacity:1000}")
    private int salesQueueCapacity;

    @Value("${workers.persistence.buys.queue-capacity:1000}")
    private int buysQueueCapacity;

    private BlockingQueue<OrderEvent> salesQueue;
    private BlockingQueue<OrderEvent> buysQueue;

    private ExecutorService salesExecutor;
    private ExecutorService buysExecutor;

    private final Timer ordenTimer;

    private final List<PersistenceWorker> workers = new ArrayList<>();

    public PersistenceWorkerPool(EventBus eventBus, OrderService orderService, Timer ordenTimer) {
        this.eventBus = eventBus;
        this.orderService = orderService;
        this.ordenTimer = ordenTimer;
    }

    @PostConstruct
    public void initialize() {
        salesQueue = new LinkedBlockingQueue<>(salesQueueCapacity);
        buysQueue = new LinkedBlockingQueue<>(buysQueueCapacity);

        salesExecutor = Executors.newFixedThreadPool(salesWorkerCount);
        buysExecutor = Executors.newFixedThreadPool(buysWorkerCount);

        for (int i = 0; i < salesWorkerCount; i++) {
            PersistenceWorker worker = new PersistenceWorker(
                    salesQueue,
                    orderService,
                    eventBus,
                    "SalesPersistenceWorker-" + i,
                    ordenTimer
            );
            workers.add(worker);
            salesExecutor.execute(worker);
        }

        for (int i = 0; i < buysWorkerCount; i++) {
            PersistenceWorker worker = new PersistenceWorker(
                    buysQueue,
                    orderService,
                    eventBus,
                    "BuysPersistenceWorker-" + i,
                    ordenTimer
            );
            workers.add(worker);
            buysExecutor.execute(worker);
        }

        eventBus.subscribe(this::handleEvent);

        log.info("PersistenceWorkerPool initialized: {} sale workers, {} buy workers",
                salesWorkerCount, buysWorkerCount);
    }

    private void handleEvent(OrderEvent event) {
        if (event.getType() == EventType.SALE_RECEIVED) {
            boolean offered = salesQueue.offer(event);
            if (!offered) {
                log.error("Sales queue full, dropping order: {}", event.getOrder().getId());
            }
        } else if (event.getType() == EventType.BUY_RECEIVED) {
            boolean offered = buysQueue.offer(event);
            if (!offered) {
                log.error("Buys queue full, dropping order: {}", event.getOrder().getId());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down PersistenceWorkerPool");

        salesExecutor.shutdownNow();
        buysExecutor.shutdownNow();

        log.info("PersistenceWorkerPool shutdown complete");
    }

    public int getSalesQueueSize() {
        return salesQueue.size();
    }

    public int getBuysQueueSize() {
        return buysQueue.size();
    }
}