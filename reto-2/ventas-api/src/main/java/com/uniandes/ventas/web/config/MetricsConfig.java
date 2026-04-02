package com.uniandes.ventas.web.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    @Bean(name = "ordenTimer")
    public Timer ordenTimer(MeterRegistry registry) {
        return Timer.builder("orden.tiempo.total")
                .publishPercentiles(0.5, 0.95)
                .publishPercentileHistogram(true)
                .register(registry);
    }
}
