package com.manacommunity.api.analytics.olap;

import com.manacommunity.api.analytics.olap.model.GatePassageEventDto;
import com.manacommunity.api.analytics.olap.model.GateThroughputMetricsDto;
import com.manacommunity.api.analytics.olap.model.GuardEfficiencyReportDto;
import com.manacommunity.api.analytics.olap.model.PeakDeliveryHourAnalysisDto;
import com.manacommunity.api.analytics.olap.service.GateOlapAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GateOlapAnalyticsServiceTest {

    private GateOlapAnalyticsServiceImpl service;
    private final Long communityId = 2001L;
    private final LocalDate today = LocalDate.of(2026, 11, 27);

    @BeforeEach
    void setUp() {
        service = new GateOlapAnalyticsServiceImpl();
        service.clearBuffer();
    }

    @Test
    @DisplayName("Should aggregate gate throughput metrics and compute approval percentiles without DB queries")
    void testGateMetricsAggregationAndPercentiles() {
        List<GatePassageEventDto> events = new ArrayList<>();

        // Generate 100 entries for GATE-MAIN
        for (int i = 1; i <= 100; i++) {
            events.add(GatePassageEventDto.builder()
                    .eventId(UUID.randomUUID())
                    .communityId(communityId)
                    .gateId("GATE-MAIN")
                    .guardId(10L)
                    .visitorType(i % 2 == 0 ? "DELIVERY" : "GUEST")
                    .entryStatus("APPROVED")
                    .gatePassageTimestamp(today.atTime(10, (i % 60)))
                    .approvalDurationSeconds(i) // 1s to 100s
                    .isPreAuthorized(i <= 40)
                    .build());
        }

        service.ingestBatch(events);

        GateThroughputMetricsDto metrics = service.getGateMetrics(communityId, "GATE-MAIN", today);

        assertNotNull(metrics);
        assertEquals(100, metrics.getTotalPassages());
        assertEquals(40, metrics.getPreAuthorizedCount());
        assertEquals(50, metrics.getDeliveryPassages());
        assertEquals(50, metrics.getGuestPassages());
        assertEquals(50.5, metrics.getAvgApprovalSeconds(), 0.1);
        assertEquals(95.0, metrics.getP95ApprovalSeconds(), 1.0);
        assertFalse(metrics.isSlaBreached());
    }

    @Test
    @DisplayName("Should identify primary and secondary peak delivery hours")
    void testPeakDeliveryHoursAnalysis() {
        List<GatePassageEventDto> deliveries = new ArrayList<>();

        // 50 deliveries at 19:00 (Evening rush)
        for (int i = 0; i < 50; i++) {
            deliveries.add(createDeliveryEvent(today.atTime(19, (i % 60))));
        }
        // 30 deliveries at 13:00 (Lunch spike)
        for (int i = 0; i < 30; i++) {
            deliveries.add(createDeliveryEvent(today.atTime(13, (i % 60))));
        }
        // 10 deliveries at 09:00 (Morning)
        for (int i = 0; i < 10; i++) {
            deliveries.add(createDeliveryEvent(today.atTime(9, (i % 60))));
        }

        service.ingestBatch(deliveries);

        PeakDeliveryHourAnalysisDto analysis = service.analyzePeakDeliveryHours(communityId, today);

        assertNotNull(analysis);
        assertEquals(19, analysis.getPrimaryPeakHour());
        assertEquals(50, analysis.getPrimaryPeakVolume());
        assertEquals(13, analysis.getSecondaryPeakHour());
        assertEquals(30, analysis.getSecondaryPeakVolume());
        assertTrue(analysis.getPeakToAverageRatio() > 1.0);
        assertEquals(24, analysis.getHourlyBreakdown().size());
    }

    @Test
    @DisplayName("Should flag slow gates exceeding approval SLA threshold")
    void testGuardEfficiencyReportAndSlaViolation() {
        List<GatePassageEventDto> events = new ArrayList<>();

        // GATE-FAST: average approval 30s
        for (int i = 0; i < 20; i++) {
            events.add(GatePassageEventDto.builder()
                    .eventId(UUID.randomUUID())
                    .communityId(communityId)
                    .gateId("GATE-FAST")
                    .visitorType("DELIVERY")
                    .gatePassageTimestamp(today.atTime(11, 0))
                    .approvalDurationSeconds(30.0)
                    .isPreAuthorized(true)
                    .build());
        }

        // GATE-SLOW: average approval 150s (Breaches SLA of 120s)
        for (int i = 0; i < 20; i++) {
            events.add(GatePassageEventDto.builder()
                    .eventId(UUID.randomUUID())
                    .communityId(communityId)
                    .gateId("GATE-SLOW")
                    .visitorType("GUEST")
                    .gatePassageTimestamp(today.atTime(11, 0))
                    .approvalDurationSeconds(150.0)
                    .isPreAuthorized(false)
                    .build());
        }

        service.ingestBatch(events);

        GuardEfficiencyReportDto report = service.generateGuardEfficiencyReport(communityId, today, 120.0);

        assertNotNull(report);
        assertEquals(2, report.getGateBreakdowns().size());
        assertEquals(1, report.getFlaggedSlowGates().size());
        assertTrue(report.getFlaggedSlowGates().contains("GATE-SLOW"));
        assertEquals(90.0, report.getOverallAverageApprovalSeconds(), 0.1);
    }

    @Test
    @DisplayName("Should generate production ClickHouse DDL and real-time analytical SQL")
    void testClickHouseDdlGeneration() {
        String sql = service.generateClickHouseDdlAndQueries(communityId);
        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS kafka_gate_passages"));
        assertTrue(sql.contains("ENGINE = AggregatingMergeTree()"));
        assertTrue(sql.contains("WHERE community_id = " + communityId));
    }

    private GatePassageEventDto createDeliveryEvent(LocalDateTime time) {
        return GatePassageEventDto.builder()
                .eventId(UUID.randomUUID())
                .communityId(communityId)
                .gateId("GATE-MAIN")
                .guardId(5L)
                .visitorType("DELIVERY")
                .entryStatus("APPROVED")
                .gatePassageTimestamp(time)
                .approvalDurationSeconds(25.0)
                .isPreAuthorized(true)
                .build();
    }
}
