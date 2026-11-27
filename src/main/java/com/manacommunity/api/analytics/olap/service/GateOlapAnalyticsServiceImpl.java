package com.manacommunity.api.analytics.olap.service;

import com.manacommunity.api.analytics.olap.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GateOlapAnalyticsServiceImpl implements GateOlapAnalyticsService {

    // Simulates ClickHouse partitioned aggregate storage: Map<CommunityId, Map<LocalDate, List<GatePassageEventDto>>>
    private final Map<Long, Map<LocalDate, List<GatePassageEventDto>>> eventStorage = new ConcurrentHashMap<>();

    @Override
    public void ingestPassageEvent(GatePassageEventDto event) {
        if (event == null || event.getCommunityId() == null || event.getGatePassageTimestamp() == null) {
            return;
        }
        LocalDate date = event.getGatePassageTimestamp().toLocalDate();
        eventStorage
                .computeIfAbsent(event.getCommunityId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(date, k -> new CopyOnWriteArrayList<>())
                .add(event);
    }

    @Override
    public void ingestBatch(List<GatePassageEventDto> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (GatePassageEventDto event : events) {
            ingestPassageEvent(event);
        }
    }

    @Override
    public GateThroughputMetricsDto getGateMetrics(Long communityId, String gateId, LocalDate date) {
        List<GatePassageEventDto> events = getEvents(communityId, date);
        List<GatePassageEventDto> filtered = events.stream()
                .filter(e -> gateId == null || gateId.equalsIgnoreCase(e.getGateId()))
                .collect(Collectors.toList());

        return computeMetrics(communityId, gateId, date, filtered);
    }

    @Override
    public GateThroughputMetricsDto getSocietyAggregateMetrics(Long communityId, LocalDate date) {
        List<GatePassageEventDto> events = getEvents(communityId, date);
        return computeMetrics(communityId, "ALL_GATES", date, events);
    }

    @Override
    public PeakDeliveryHourAnalysisDto analyzePeakDeliveryHours(Long communityId, LocalDate date) {
        List<GatePassageEventDto> events = getEvents(communityId, date);
        List<GatePassageEventDto> deliveries = events.stream()
                .filter(e -> "DELIVERY".equalsIgnoreCase(e.getVisitorType()))
                .collect(Collectors.toList());

        Map<Integer, Long> hourlyCounts = new HashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyCounts.put(h, 0L);
        }

        for (GatePassageEventDto d : deliveries) {
            int hour = d.getGatePassageTimestamp().getHour();
            hourlyCounts.put(hour, hourlyCounts.get(hour) + 1);
        }

        long totalDeliveries = deliveries.size();
        List<Map.Entry<Integer, Long>> sortedHours = hourlyCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        int primaryPeakHour = sortedHours.isEmpty() ? 0 : sortedHours.get(0).getKey();
        long primaryPeakVolume = sortedHours.isEmpty() ? 0 : sortedHours.get(0).getValue();

        int secondaryPeakHour = sortedHours.size() > 1 ? sortedHours.get(1).getKey() : 0;
        long secondaryPeakVolume = sortedHours.size() > 1 ? sortedHours.get(1).getValue() : 0;

        double avgHourly = totalDeliveries > 0 ? (double) totalDeliveries / 24.0 : 0.0;
        double peakToAvgRatio = avgHourly > 0 ? round(primaryPeakVolume / avgHourly, 2) : 0.0;

        List<PeakDeliveryHourAnalysisDto.HourlyBucket> buckets = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            long count = hourlyCounts.get(h);
            double pct = totalDeliveries > 0 ? round((count * 100.0) / totalDeliveries, 2) : 0.0;
            buckets.add(PeakDeliveryHourAnalysisDto.HourlyBucket.builder()
                    .hour(h)
                    .totalDeliveries(count)
                    .percentageOfDailyDeliveries(pct)
                    .build());
        }

        return PeakDeliveryHourAnalysisDto.builder()
                .communityId(communityId)
                .reportDate(date)
                .primaryPeakHour(primaryPeakHour)
                .primaryPeakVolume(primaryPeakVolume)
                .secondaryPeakHour(secondaryPeakHour)
                .secondaryPeakVolume(secondaryPeakVolume)
                .peakToAverageRatio(peakToAvgRatio)
                .hourlyBreakdown(buckets)
                .build();
    }

    @Override
    public GuardEfficiencyReportDto generateGuardEfficiencyReport(Long communityId, LocalDate date, double slaThresholdSeconds) {
        List<GatePassageEventDto> events = getEvents(communityId, date);
        Map<String, List<GatePassageEventDto>> byGate = events.stream()
                .collect(Collectors.groupingBy(GatePassageEventDto::getGateId));

        List<GuardEfficiencyReportDto.GateEfficiencyItem> items = new ArrayList<>();
        List<String> flaggedSlowGates = new ArrayList<>();
        List<Double> allDurations = new ArrayList<>();

        for (Map.Entry<String, List<GatePassageEventDto>> entry : byGate.entrySet()) {
            String gId = entry.getKey();
            List<GatePassageEventDto> gateEvents = entry.getValue();

            List<Double> durations = gateEvents.stream()
                    .map(GatePassageEventDto::getApprovalDurationSeconds)
                    .filter(d -> d > 0)
                    .sorted()
                    .collect(Collectors.toList());

            allDurations.addAll(durations);

            double avgSec = durations.isEmpty() ? 0.0 : round(durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0), 2);
            double p95Sec = calculatePercentile(durations, 0.95);

            long preAuth = gateEvents.stream().filter(GatePassageEventDto::isPreAuthorized).count();
            double preAuthRatio = gateEvents.isEmpty() ? 0.0 : round((double) preAuth / gateEvents.size(), 2);

            boolean slaViolated = avgSec > slaThresholdSeconds || p95Sec > (slaThresholdSeconds * 1.5);
            if (slaViolated) {
                flaggedSlowGates.add(gId);
            }

            items.add(GuardEfficiencyReportDto.GateEfficiencyItem.builder()
                    .gateId(gId)
                    .totalEntries(gateEvents.size())
                    .avgApprovalSeconds(avgSec)
                    .p95ApprovalSeconds(p95Sec)
                    .preAuthorizedRatio(preAuthRatio)
                    .slaViolated(slaViolated)
                    .build());
        }

        allDurations.sort(Double::compareTo);
        double overallAvg = allDurations.isEmpty() ? 0.0 : round(allDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0), 2);
        double overallP95 = calculatePercentile(allDurations, 0.95);

        return GuardEfficiencyReportDto.builder()
                .communityId(communityId)
                .reportDate(date)
                .gateBreakdowns(items)
                .overallAverageApprovalSeconds(overallAvg)
                .overallP95ApprovalSeconds(overallP95)
                .flaggedSlowGates(flaggedSlowGates)
                .build();
    }

    @Override
    public String generateClickHouseDdlAndQueries(Long communityId) {
        return "-- ClickHouse Production OLAP Ingestion for 30,000 Societies\n" +
                "-- Target: Zero load on PostgreSQL OLTP\n\n" +
                "CREATE TABLE IF NOT EXISTS kafka_gate_passages (\n" +
                "    event_id UUID,\n" +
                "    community_id UInt32,\n" +
                "    gate_id LowCardinality(String),\n" +
                "    guard_id UInt32,\n" +
                "    visitor_type LowCardinality(String),\n" +
                "    entry_status LowCardinality(String),\n" +
                "    request_timestamp DateTime64(3, 'UTC'),\n" +
                "    approval_timestamp Nullable(DateTime64(3, 'UTC')),\n" +
                "    gate_passage_timestamp DateTime64(3, 'UTC'),\n" +
                "    approval_duration_seconds Float32,\n" +
                "    is_pre_authorized UInt8\n" +
                ") ENGINE = Kafka\n" +
                "SETTINGS kafka_broker_list = 'kafka:9092',\n" +
                "         kafka_topic_list = 'gate.passage.events.v1',\n" +
                "         kafka_group_name = 'clickhouse_gate_analytics_group',\n" +
                "         kafka_format = 'JSONEachRow';\n\n" +
                "CREATE TABLE IF NOT EXISTS gate_hourly_throughput_summary (\n" +
                "    event_date Date,\n" +
                "    community_id UInt32,\n" +
                "    gate_id LowCardinality(String),\n" +
                "    visitor_type LowCardinality(String),\n" +
                "    hour_slot UInt8,\n" +
                "    total_passages UInt32,\n" +
                "    pre_authorized_count UInt32,\n" +
                "    approval_time_avg AggregateFunction(avg, Float32),\n" +
                "    approval_time_p95 AggregateFunction(quantilesExact(0.95), Float32)\n" +
                ") ENGINE = AggregatingMergeTree()\n" +
                "PARTITION BY toYYYYMM(event_date)\n" +
                "ORDER BY (community_id, gate_id, visitor_type, event_date, hour_slot);\n\n" +
                "-- Real-time Query: Average Approval Time per Gate\n" +
                "SELECT gate_id, sum(total_passages) AS total, avgMerge(approval_time_avg) AS avg_sec, quantilesExactMerge(0.95)(approval_time_p95)[1] AS p95_sec\n" +
                "FROM gate_hourly_throughput_summary\n" +
                "WHERE community_id = " + communityId + " AND event_date = today()\n" +
                "GROUP BY gate_id ORDER BY total DESC;\n";
    }

    @Override
    public void clearBuffer() {
        eventStorage.clear();
    }

    private List<GatePassageEventDto> getEvents(Long communityId, LocalDate date) {
        Map<LocalDate, List<GatePassageEventDto>> dateMap = eventStorage.get(communityId);
        if (dateMap == null) {
            return Collections.emptyList();
        }
        List<GatePassageEventDto> list = dateMap.get(date);
        return list == null ? Collections.emptyList() : list;
    }

    private GateThroughputMetricsDto computeMetrics(Long communityId, String gateId, LocalDate date, List<GatePassageEventDto> events) {
        long total = events.size();
        long preAuth = 0;
        long deliveries = 0;
        long cabs = 0;
        long guests = 0;
        long dailyHelp = 0;

        Map<Integer, Long> hourlyTotal = new HashMap<>();
        Map<Integer, Long> hourlyDelivery = new HashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyTotal.put(h, 0L);
            hourlyDelivery.put(h, 0L);
        }

        List<Double> durations = new ArrayList<>();

        for (GatePassageEventDto e : events) {
            if (e.isPreAuthorized()) preAuth++;

            String type = e.getVisitorType() != null ? e.getVisitorType().toUpperCase() : "UNKNOWN";
            switch (type) {
                case "DELIVERY":
                    deliveries++;
                    int dHour = e.getGatePassageTimestamp().getHour();
                    hourlyDelivery.put(dHour, hourlyDelivery.get(dHour) + 1);
                    break;
                case "CAB":
                    cabs++;
                    break;
                case "GUEST":
                    guests++;
                    break;
                case "DAILY_HELP":
                    dailyHelp++;
                    break;
            }

            int hour = e.getGatePassageTimestamp().getHour();
            hourlyTotal.put(hour, hourlyTotal.get(hour) + 1);

            if (e.getApprovalDurationSeconds() > 0) {
                durations.add(e.getApprovalDurationSeconds());
            }
        }

        durations.sort(Double::compareTo);
        double avgSec = durations.isEmpty() ? 0.0 : round(durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0), 2);
        double p95Sec = calculatePercentile(durations, 0.95);

        int peakDelivHour = 0;
        long peakDelivVol = 0;
        for (Map.Entry<Integer, Long> entry : hourlyDelivery.entrySet()) {
            if (entry.getValue() > peakDelivVol) {
                peakDelivVol = entry.getValue();
                peakDelivHour = entry.getKey();
            }
        }

        boolean slaBreached = avgSec > 120.0;

        return GateThroughputMetricsDto.builder()
                .communityId(communityId)
                .gateId(gateId)
                .eventDate(date)
                .totalPassages(total)
                .preAuthorizedCount(preAuth)
                .avgApprovalSeconds(avgSec)
                .p95ApprovalSeconds(p95Sec)
                .deliveryPassages(deliveries)
                .cabPassages(cabs)
                .guestPassages(guests)
                .dailyHelpPassages(dailyHelp)
                .hourlyDistribution(hourlyTotal)
                .hourlyDeliveryDistribution(hourlyDelivery)
                .peakDeliveryHour(peakDelivHour)
                .peakDeliveryHourVolume(peakDelivVol)
                .slaBreached(slaBreached)
                .build();
    }

    private double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return round(sortedValues.get(index), 2);
    }

    private double round(double val, int places) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        return BigDecimal.valueOf(val).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
