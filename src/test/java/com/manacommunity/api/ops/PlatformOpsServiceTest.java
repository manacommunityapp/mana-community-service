package com.manacommunity.api.ops;

import com.manacommunity.api.ops.dto.OpsDashboardDtos.*;
import com.manacommunity.api.ops.service.PlatformOpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Platform Operations Service & Collector Tests")
class PlatformOpsServiceTest {

    private OpsMetricsCollector collector;
    private PlatformOpsService opsService;

    @BeforeEach
    void setUp() {
        collector = new OpsMetricsCollector();
        collector.resetMetrics();
        opsService = new PlatformOpsService(collector, Optional.empty(), Optional.empty());
    }

    @Test
    @DisplayName("Should track requests, 5xx errors and calculate sliding error rate correctly")
    void testMetricsCollector() {
        // Record 10 successful requests
        for (int i = 0; i < 10; i++) {
            collector.recordRequest("GET", "/api/communities", 200, 15, "cid-" + i, "127.0.0.1", null);
        }

        // Record 2 client errors (4xx)
        collector.recordRequest("GET", "/api/invalid", 404, 5, "cid-404", "127.0.0.1", "Not found");
        collector.recordRequest("POST", "/api/auth/login", 401, 8, "cid-401", "127.0.0.1", "Bad credentials");

        // Record 1 server error (500)
        collector.recordRequest("POST", "/api/checkout", 500, 250, "cid-500", "127.0.0.1", "Database timeout");

        assertEquals(13, collector.getTotalRequests());
        assertEquals(2, collector.getTotal4xxErrors());
        assertEquals(1, collector.getTotal5xxErrors());

        List<FailedRequestDto> failures = collector.getRecentFailures();
        assertEquals(1, failures.size());
        assertEquals(500, failures.get(0).getStatusCode());
        assertEquals("/api/checkout", failures.get(0).getPath());
        assertEquals("Database timeout", failures.get(0).getErrorMessage());

        // Error rate: 1/13 * 100 = ~7.69%
        double errorRate = collector.getSlidingErrorRate();
        assertTrue(errorRate > 7.0 && errorRate < 8.0, "Expected error rate ~7.69%, got: " + errorRate);
    }

    @Test
    @DisplayName("Should aggregate multi-service health with correct statuses and cluster metrics")
    void testGetPlatformOverview() {
        PlatformOpsOverviewDto overview = opsService.getPlatformOverview();

        assertNotNull(overview);
        assertNotNull(overview.getGeneratedAt());
        assertNotNull(overview.getClusterMetrics());
        assertNotNull(overview.getServices());
        assertNotNull(overview.getQueueLags());
        assertNotNull(overview.getMigrations());

        // Verify services probed include Core, DB, Redis, Kafka, Nginx
        List<ServiceHealthDto> services = overview.getServices();
        assertTrue(services.stream().anyMatch(s -> "core-service".equals(s.getId())));
        assertTrue(services.stream().anyMatch(s -> "postgres-db".equals(s.getId())));
        assertTrue(services.stream().anyMatch(s -> "redis-cache".equals(s.getId())));
        assertTrue(services.stream().anyMatch(s -> "kafka-cluster".equals(s.getId())));
        assertTrue(services.stream().anyMatch(s -> "nginx-gateway".equals(s.getId())));

        // Verify Kafka queue lags
        List<QueueLagDto> lags = overview.getQueueLags();
        assertFalse(lags.isEmpty());
        assertTrue(lags.stream().anyMatch(q -> q.getTopic().contains("anpr")));
        assertTrue(lags.stream().anyMatch(q -> q.getTopic().contains("wallet")));

        // Verify Liquibase migrations
        List<MigrationStatusDto> migrations = overview.getMigrations();
        assertEquals(3, migrations.size());
        assertTrue(migrations.stream().allMatch(m -> "UP_TO_DATE".equals(m.getStatus())));
    }

    @Test
    @DisplayName("Should evaluate and trigger alerts when incident conditions occur")
    void testIncidentAlertEvaluation() {
        ServiceHealthDto downService = ServiceHealthDto.builder()
                .id("test-service")
                .name("Test Down Service")
                .status(HealthStatus.DOWN)
                .port(9999)
                .details("Connection refused")
                .latencyMs(1200)
                .build();

        SystemClusterMetricsDto highErrorMetrics = SystemClusterMetricsDto.builder()
                .overallErrorRate(12.5)
                .build();

        QueueLagDto highLag = QueueLagDto.builder()
                .topic("high.lag.topic")
                .consumerGroup("test-group")
                .totalLag(1500)
                .targetService("test-svc")
                .build();

        List<IncidentAlertDto> incidents = opsService.evaluateIncidents(
                List.of(downService),
                highErrorMetrics,
                List.of(highLag)
        );

        assertEquals(3, incidents.size());
        assertTrue(incidents.stream().anyMatch(i -> i.getTitle().contains("Test Down Service")));
        assertTrue(incidents.stream().anyMatch(i -> i.getTitle().contains("Elevated 5xx Error Rate")));
        assertTrue(incidents.stream().anyMatch(i -> i.getTitle().contains("Kafka Consumer Lag Backlog")));

        // Acknowledge incident
        boolean ack = opsService.acknowledgeIncident("INC-DOWN-test-service", "superadmin@mana.com");
        assertTrue(ack);

        // After acknowledgment, should not re-alert on the same incident ID
        List<IncidentAlertDto> incidentsAfterAck = opsService.evaluateIncidents(
                List.of(downService),
                SystemClusterMetricsDto.builder().overallErrorRate(0.0).build(),
                List.of()
        );
        assertEquals(0, incidentsAfterAck.size());
    }
}