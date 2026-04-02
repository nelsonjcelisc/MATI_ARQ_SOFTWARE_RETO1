package com.uniandes.matching.events.workers.notifications;

import com.uniandes.matching.domain.service.NotificationService;
import com.uniandes.matching.events.bus.EventBus;
import com.uniandes.matching.events.model.EventType;
import com.uniandes.matching.events.model.OrderEvent;
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
public class NotificationWorkerPool {

    private final EventBus eventBus;
    private final NotificationService notificationService;

    @Value("${workers.notifications.count:10}")
    private int workerCount;

    @Value("${workers.notifications.queue-capacity:1000}")
    private int queueCapacity;

    private BlockingQueue<OrderEvent> queue;
    private ExecutorService executor;
    private final List<NotificationWorker> workers = new ArrayList<>();

    public NotificationWorkerPool(EventBus eventBus, NotificationService notificationService) {
        this.eventBus = eventBus;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void initialize() {
        queue = new LinkedBlockingQueue<>(queueCapacity);
        executor = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            NotificationWorker worker = new NotificationWorker(
                    queue,
                    notificationService,
                    "NotificationWorker-" + i
            );
            workers.add(worker);
            executor.execute(worker);
        }

        eventBus.subscribe(this::handleEvent);

        log.info("NotificationWorkerPool initialized: {} workers", workerCount);
    }

    private void handleEvent(OrderEvent event) {
        if (event.getType() == EventType.SALE_RECEIVED ||
                event.getType() == EventType.BUY_RECEIVED) {

            boolean offered = queue.offer(event);
            if (!offered) {
                log.error("Notification queue full, dropping notification for order: {}",
                        event.getOrder().getId());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down NotificationWorkerPool");
        executor.shutdownNow();
        log.info("NotificationWorkerPool shutdown complete");
    }

    public int getQueueSize() {
        return queue.size();
    }
}