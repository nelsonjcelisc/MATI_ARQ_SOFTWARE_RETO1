package com.uniandes.matching.web.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.Timer;

@Configuration

public class MetricsConfig {

    @Bean
    public Timer ordenTimer(MeterRegistry registry) {
        return Timer.builder("orden.tiempo.total")
                .description("Tiempo total de procesamiento de órdenes de compra/venta")
                .tag("app", "matching-engine")
                .publishPercentiles(0.5, 0.95, 0.99)  // p50, p95, p99
                .publishPercentileHistogram(true)      // Histograma para Prometheus
                .register(registry);
    }
}

