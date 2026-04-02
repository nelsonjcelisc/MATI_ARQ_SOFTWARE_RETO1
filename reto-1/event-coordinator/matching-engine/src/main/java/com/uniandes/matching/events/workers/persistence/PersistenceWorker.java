package com.uniandes.matching.events.workers.persistence;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.service.OrderService;
import com.uniandes.matching.events.bus.EventBus;
import com.uniandes.matching.events.model.EventType;
import com.uniandes.matching.events.model.OrderEvent;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PersistenceWorker implements Runnable {

    private final BlockingQueue<OrderEvent> queue;
    private final OrderService orderService;
    private final EventBus eventBus;
    private final String workerName;
    private final Timer ordenTimer;

    public PersistenceWorker(BlockingQueue<OrderEvent> queue,
                             OrderService orderService,
                             EventBus eventBus,
                             String workerName,
                             Timer ordenTimer) {
        this.queue = queue;
        this.orderService = orderService;
        this.eventBus = eventBus;
        this.workerName = workerName;
        this.ordenTimer = ordenTimer;
    }

    @Override
    public void run() {
        log.info("{} started", workerName);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderEvent event = queue.take();

                log.debug("{} processing order: {}", workerName, event.getOrder().getId());

                Instant startTime = event.getTiempo();

                orderService.createOrder(event.getOrder());

                //Calcula la duración TOTAL (desde Controller hasta DB)
                Duration totalDuration = Duration.between(startTime, Instant.now());

                //Registra en Micrometer
                ordenTimer.record(totalDuration.toMillis(), TimeUnit.MILLISECONDS);

                log.debug("{} persisted order {} - Total time: {}ms",
                        workerName,
                        event.getOrder().getId(),
                        totalDuration.toMillis());

                eventBus.publish(OrderEvent.of(EventType.ORDER_PERSISTED, event.getOrder()));

            } catch (InterruptedException e) {
                log.warn("{} interrupted, shutting down", workerName);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("{} error processing order: {}", workerName, e.getMessage(), e);
            }
        }

        log.info("{} stopped", workerName);
    }
}