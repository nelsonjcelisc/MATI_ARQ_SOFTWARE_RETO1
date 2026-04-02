package com.uniandes.matching.events.bus;

import com.uniandes.matching.events.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Component
public class EventBus {

    private final List<Consumer<OrderEvent>> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<OrderEvent> subscriber) {
        subscribers.add(subscriber);
        log.info("New subscriber registered. Total: {}", subscribers.size());
    }

    public void publish(OrderEvent event) {
        log.debug("Publishing event: {} for order: {}", event.getType(), event.getOrder().getId());

        for (Consumer<OrderEvent> subscriber : subscribers) {
            try {
                subscriber.accept(event);
            } catch (Exception e) {
                log.error("Error in subscriber processing event {}: {}",
                        event.getType(), e.getMessage(), e);
            }
        }
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }
}