package com.manacommunity.api.analytics.olap.service;

import com.manacommunity.api.analytics.olap.model.GatePassageEventDto;
import com.manacommunity.api.analytics.olap.model.GateThroughputMetricsDto;
import com.manacommunity.api.analytics.olap.model.GuardEfficiencyReportDto;
import com.manacommunity.api.analytics.olap.model.PeakDeliveryHourAnalysisDto;

import java.time.LocalDate;
import java.util.List;

public interface GateOlapAnalyticsService {

    void ingestPassageEvent(GatePassageEventDto event);

    void ingestBatch(List<GatePassageEventDto> events);

    GateThroughputMetricsDto getGateMetrics(Long communityId, String gateId, LocalDate date);

    GateThroughputMetricsDto getSocietyAggregateMetrics(Long communityId, LocalDate date);

    PeakDeliveryHourAnalysisDto analyzePeakDeliveryHours(Long communityId, LocalDate date);

    GuardEfficiencyReportDto generateGuardEfficiencyReport(Long communityId, LocalDate date, double slaThresholdSeconds);

    String generateClickHouseDdlAndQueries(Long communityId);

    void clearBuffer();
}
