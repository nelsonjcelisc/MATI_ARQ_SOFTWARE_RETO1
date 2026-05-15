package com.uniandes.registro.lamina.web.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MetricsConfig {
    @Bean
    public MeterFilter ordenTimerFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("registro.tiempo.total")) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(0.5, 0.95)
                            .percentilesHistogram(true)
                            .serviceLevelObjectives(
                                    Duration.ofMillis(10).toNanos(),  // Muy rápido
                                    Duration.ofMillis(50).toNanos(),  // Normal
                                    Duration.ofMillis(100).toNanos(), // Límite deseado
                                    Duration.ofMillis(200).toNanos(), // Un poco lento
                                    Duration.ofMillis(500).toNanos(), // Lento
                                    Duration.ofSeconds(1).toNanos()   // Alerta crítica
                            )
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
