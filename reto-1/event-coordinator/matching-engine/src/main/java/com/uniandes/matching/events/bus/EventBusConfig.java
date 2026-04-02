package com.uniandes.matching.events.bus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EventBusConfig {

    public EventBusConfig(EventBus eventBus) {
        log.info("EventBus initialized with 0 subscribers");
    }
}