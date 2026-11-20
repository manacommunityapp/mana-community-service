package com.manacommunity.platform.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Custom HealthIndicator that exposes connection-pool pressure metrics and marks
 * the application DOWN when the pool is saturated (pending threads > 10).
 * This surfaces sooner than a full connection timeout would.
 */
@Slf4j
@Component("dbPool")
@RequiredArgsConstructor
public class DatabaseConnectionHealthIndicator implements HealthIndicator {

    private static final int PENDING_THREADS_THRESHOLD = 10;

    private final DataSource dataSource;

    @Override
    public Health health() {
        if (!(dataSource instanceof HikariDataSource hikari)) {
            return Health.up().withDetail("pool", "non-HikariCP, skipped").build();
        }

        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        if (pool == null) {
            return Health.up().withDetail("pool", "HikariCP not yet initialised").build();
        }

        int activeConnections = pool.getActiveConnections();
        int idleConnections   = pool.getIdleConnections();
        int totalConnections  = pool.getTotalConnections();
        int pendingThreads    = pool.getThreadsAwaitingConnection();

        Health.Builder builder = pendingThreads > PENDING_THREADS_THRESHOLD
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("activeConnections", activeConnections)
                .withDetail("idleConnections", idleConnections)
                .withDetail("totalConnections", totalConnections)
                .withDetail("pendingThreads", pendingThreads)
                .withDetail("maxPoolSize", hikari.getMaximumPoolSize())
                .build();
    }
}
