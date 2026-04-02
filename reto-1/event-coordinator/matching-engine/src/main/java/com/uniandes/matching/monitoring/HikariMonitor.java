package com.uniandes.matching.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@EnableScheduling
@ConditionalOnClass(HikariDataSource.class)
public class HikariMonitor {

    private final DataSource dataSource;

    public HikariMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(fixedDelay = 30000) // Cada 30 segundos
    public void logPoolStats() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();

            int active = pool.getActiveConnections();
            int idle = pool.getIdleConnections();
            int total = pool.getTotalConnections();
            int waiting = pool.getThreadsAwaitingConnection();

            log.info("┌─────────────────────────────────────────┐");
            log.info("│ HikariCP Connection Pool Stats         │");
            log.info("├─────────────────────────────────────────┤");
            log.info("│ Active:   {} / {}                      │",
                    String.format("%3d", active),
                    String.format("%3d", total));
            log.info("│ Idle:     {}                           │",
                    String.format("%3d", idle));
            log.info("│ Waiting:  {} {}                        │",
                    String.format("%3d", waiting),
                    waiting > 0 ? "⚠️ BLOCKED!" : "✓");
            log.info("└─────────────────────────────────────────┘");

            // Alerta si hay threads esperando
            if (waiting > 0) {
                log.warn("⚠️  {} threads waiting for DB connection! Consider increasing pool size",
                        waiting);
            }
        }
    }
}