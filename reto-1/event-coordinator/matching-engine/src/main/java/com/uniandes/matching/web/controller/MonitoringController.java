package com.uniandes.matching.web.controller;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final DataSource dataSource;

    public MonitoringController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/hikari")
    public Map<String, Object> getHikariStats() {
        Map<String, Object> stats = new HashMap<>();

        if (dataSource instanceof HikariDataSource hikari) {
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();

            stats.put("active", pool.getActiveConnections());
            stats.put("idle", pool.getIdleConnections());
            stats.put("total", pool.getTotalConnections());
            stats.put("waiting", pool.getThreadsAwaitingConnection());
            stats.put("maxPoolSize", hikari.getMaximumPoolSize());
        }

        return stats;
    }
}