package com.manacommunity.api.iot.metering.dto;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MeteringDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestTelemetryRequest {
        @NotBlank(message = "Meter serial number is required")
        private String meterSerialNumber;

        @NotNull(message = "Pulse count is required")
        private Long pulseCount;

        private BigDecimal instantaneousFlow; // kW or L/min
        private BigDecimal voltage;
        private BigDecimal current;
        private BigDecimal powerFactor;
        private Integer batteryLevel;
        private Integer signalRssi;
        private Boolean tamperFlag;
        private String rawPayloadJson;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModbusTelemetryBatchRequest {
        private String gatewayId;
        private List<IngestTelemetryRequest> readings;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestTelemetryResult {
        private String meterSerialNumber;
        private MeterType meterType;
        private BigDecimal deltaConsumed;
        private BigDecimal cumulativeReading;
        private boolean anomalyDetected;
        private String alertMessage;
        private MeterStatus status;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmartMeterDto {
        private Long id;
        private String meterSerialNumber;
        private MeterType meterType;
        private Long communityId;
        private String unitNumber;
        private String blockName;
        private MeterProtocol protocol;
        private BigDecimal pulseMultiplier;
        private BigDecimal lastReading;
        private LocalDateTime lastTelemetryTime;
        private MeterStatus status;
        private Integer batteryLevel;
        private boolean leakDetected;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeterBillingCycleDto {
        private Long id;
        private Long meterId;
        private String meterSerialNumber;
        private String unitNumber;
        private MeterType meterType;
        private String billingMonth;
        private BigDecimal startReading;
        private BigDecimal endReading;
        private BigDecimal totalUnitsConsumed;
        private BigDecimal slabAmount;
        private Long cfbosInvoiceId;
        private MeterBillingStatus billingStatus;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterMeterRequest {
        @NotBlank(message = "Serial number is required")
        private String meterSerialNumber;

        @NotNull(message = "Meter type is required")
        private MeterType meterType;

        @NotNull(message = "Community ID is required")
        private Long communityId;

        private String unitNumber;
        private String blockName;
        private MeterProtocol protocol;
        private BigDecimal pulseMultiplier;
        private String ipAddress;
        private String mqttTopic;
    }
}
