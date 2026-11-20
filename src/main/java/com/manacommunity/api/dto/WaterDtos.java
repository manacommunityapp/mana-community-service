package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WaterDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TankerDeliveryRequest {
        @NotNull
        private String vendorName;
        @NotNull
        private String vehicleNumber;
        @NotNull
        private BigDecimal capacityLiters;
        @NotNull
        private BigDecimal cost;
        private String invoiceNumber;
        private String driverPhone;
        private Long targetTankId;
        private String approvedBy;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterPumpRunRequest {
        @NotNull
        private Long pumpId;
        @NotNull
        private LocalDateTime startTime;
        @NotNull
        private LocalDateTime endTime;
        private BigDecimal flowRateLpm;
        private BigDecimal energyKwh;
        private String operatorName;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterTreatmentLogRequest {
        @NotNull
        private String plantType; // WTP, STP
        @NotNull
        private String operationType; // MGF_BACKWASH, ACF_BACKWASH, SOFTENER_SALT_RECHARGE, STP_AERATION_CHECK, CHLORINE_DOSING, SLUDGE_DISPOSAL
        private BigDecimal quantityUsedKg;
        private String operatorName;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterQualityTestRequest {
        @NotNull
        private String sourceLocation; // MUNICIPAL_INFLOW, BOREWELL_1, WTP_OUTLET, STP_TREATED, TOWER_A_TAP
        @NotNull
        private BigDecimal ph;
        @NotNull
        private Integer tdsPpm;
        @NotNull
        private Integer hardnessPpm;
        private BigDecimal residualChlorinePpm;
        private BigDecimal turbidityNtu;
        private String testedBy;
        private String labReportUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TankCleaningRequest {
        @NotNull
        private Long tankId;
        @NotNull
        private LocalDate lastCleanedDate;
        @NotNull
        private LocalDate nextDueDate;
        private String cleaningType; // MECHANIZED_DEWATERING_UV, CHEMICAL_DISINFECTION, MANUAL_SCRUB
        private String vendorName;
        private BigDecimal cost;
        private String certificateUrl;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterOverviewSummaryDto {
        private BigDecimal totalCapacityLiters;
        private BigDecimal currentStorageLiters;
        private BigDecimal currentStoragePercentage;
        private Long totalTankersThisMonth;
        private BigDecimal totalTankerExpenditureThisMonth;
        private Long totalPumpsCount;
        private Long runningPumpsCount;
        private Boolean lowStorageAlert;
    }
}
