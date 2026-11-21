package com.manacommunity.api.iot.mesh.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class IoTMeshDtos {

    public record RegisterSensorRequest(
            Long communityId,
            String sensorName,
            String sensorType,
            String location,
            String unit,
            String vendor,
            String serialNumber,
            String apiEndpoint,
            String apiKeyRef,
            Integer pollingIntervalSeconds
    ) {}

    public record SensorResponse(
            Long id,
            Long communityId,
            String sensorName,
            String sensorType,
            String location,
            String unit,
            String vendor,
            boolean active,
            BigDecimal lastReading,
            Instant lastReadingAt
    ) {}

    public record IngestReadingRequest(
            BigDecimal value,
            Instant readingAt,
            String quality
    ) {}

    public record TelemetryPoint(
            BigDecimal value,
            Instant readingAt,
            String quality
    ) {}

    public record DailyAggregate(
            LocalDate date,
            BigDecimal min,
            BigDecimal max,
            BigDecimal avg,
            BigDecimal sum,
            long count
    ) {}

    public record AlertConfigRequest(
            String alertType,
            BigDecimal thresholdValue,
            BigDecimal rateChangePct,
            Integer windowMinutes,
            String severity,
            String notifyRoles
    ) {}

    public record AnomalyEventResponse(
            Long id,
            Long sensorId,
            String sensorName,
            String eventType,
            BigDecimal detectedValue,
            BigDecimal thresholdValue,
            String severity,
            String description,
            Instant detectedAt,
            boolean resolved
    ) {}

    public record EmergencyBroadcastRequest(
            String message,
            String severity
    ) {}
}
