package com.uniandes.matching.events.workers.notifications;

import com.uniandes.matching.domain.service.NotificationService;
import com.uniandes.matching.events.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class NotificationWorker implements Runnable {

    private final BlockingQueue<OrderEvent> queue;
    private final NotificationService notificationService;
    private final String workerName;

    public NotificationWorker(BlockingQueue<OrderEvent> queue,
                              NotificationService notificationService,
                              String workerName) {
        this.queue = queue;
        this.notificationService = notificationService;
        this.workerName = workerName;
    }

    @Override
    public void run() {
        log.info("{} started", workerName);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderEvent event = queue.take();

                log.debug("{} processing notification for order: {}",
                        workerName, event.getOrder().getId());

                long startTime = System.currentTimeMillis();

                notificationService.broadcastNewOrder(event.getOrder());

                long elapsed = System.currentTimeMillis() - startTime;

                log.debug("{} broadcast completed in {}ms", workerName, elapsed);

            } catch (InterruptedException e) {
                log.warn("{} interrupted, shutting down", workerName);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("{} error broadcasting: {}", workerName, e.getMessage(), e);
            }
        }

        log.info("{} stopped", workerName);
    }
}