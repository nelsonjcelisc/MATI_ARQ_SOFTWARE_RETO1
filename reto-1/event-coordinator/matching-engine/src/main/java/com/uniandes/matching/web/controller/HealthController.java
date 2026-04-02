package com.uniandes.matching.web.controller;

import com.uniandes.matching.events.bus.EventBus;
import com.uniandes.matching.events.workers.notifications.NotificationWorkerPool;
import com.uniandes.matching.events.workers.persistence.PersistenceWorkerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final EventBus eventBus;
    private final PersistenceWorkerPool persistenceWorkerPool;
    private final NotificationWorkerPool notificationWorkerPool;

    public HealthController(EventBus eventBus,
                            PersistenceWorkerPool persistenceWorkerPool,
                            NotificationWorkerPool notificationWorkerPool) {
        this.eventBus = eventBus;
        this.persistenceWorkerPool = persistenceWorkerPool;
        this.notificationWorkerPool = notificationWorkerPool;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("eventBusSubscribers", eventBus.getSubscriberCount());
        health.put("salesQueueSize", persistenceWorkerPool.getSalesQueueSize());
        health.put("buysQueueSize", persistenceWorkerPool.getBuysQueueSize());
        health.put("notificationQueueSize", notificationWorkerPool.getQueueSize());

        return ResponseEntity.ok(health);
    }
}