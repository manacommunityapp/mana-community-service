package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GeneratorDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratorRunRequest {
        @NotNull
        private Long generatorId;
        @NotNull
        private LocalDateTime startTime;
        @NotNull
        private LocalDateTime endTime;
        @NotNull
        private BigDecimal startFuelLiters;
        @NotNull
        private BigDecimal endFuelLiters;
        @NotNull
        private BigDecimal unitsGeneratedKwh;
        private String reason; // GRID_OUTAGE, SCHEDULED_TEST_RUN, MAINTENANCE_RUN, PEAK_SHAVING
        private String operatorName;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratorRefillRequest {
        @NotNull
        private Long generatorId;
        @NotNull
        private LocalDate refillDate;
        @NotNull
        private BigDecimal quantityLiters;
        @NotNull
        private BigDecimal pricePerLiter;
        private String supplierName;
        private String billNumber;
        private String enteredBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratorServiceRecordRequest {
        @NotNull
        private Long generatorId;
        @NotNull
        private String serviceType; // A_CHECK_ROUTINE, B_CHECK_250H, C_CHECK_MAJOR, EMERGENCY_REPAIR, BATTERY_REPLACEMENT
        @NotNull
        private LocalDate serviceDate;
        @NotNull
        private BigDecimal runHoursAtService;
        private String vendorName;
        private String technicianName;
        private BigDecimal cost;
        private String partsReplaced;
        private String notes;
        private String status;
        private String ticketId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratorAmcRequest {
        @NotNull
        private Long generatorId;
        @NotNull
        private String vendorName;
        @NotNull
        private String contractNumber;
        @NotNull
        private LocalDate startDate;
        @NotNull
        private LocalDate endDate;
        @NotNull
        private BigDecimal annualCost;
        private Integer visitsIncluded;
        private Integer visitsCompleted;
        private String emergencyHelpline;
        private Integer slaHours;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceAlertDto {
        private Long generatorId;
        private String generatorName;
        private BigDecimal hoursSinceLastService;
        private Integer intervalHours;
        private BigDecimal hoursRemainingOrOverdue;
        private boolean isOverdue;
        private String alertSeverity; // NORMAL, UPCOMING, OVERDUE
        private String recommendedService;
    }
}
