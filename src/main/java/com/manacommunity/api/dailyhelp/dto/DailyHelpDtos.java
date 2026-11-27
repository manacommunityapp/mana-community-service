package com.manacommunity.api.dailyhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DailyHelpDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateDwellIntervalDto {
        private Long communityId;
        private String staffUuid;
        private LocalDate workDate;
        private LocalTime gateEntryTime;
        private LocalTime gateExitTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlatAttendanceReconciliationDto {
        private Long contractId;
        private Long flatId;
        private String staffName;
        private String serviceType;
        private LocalDate workDate;
        private String status; // PRESENT, ABSENT, HALF_DAY, GATE_ABSENT
        private int scheduledMinutes;
        private int actualDwellMinutes;
        private BigDecimal proRataMultiplier;
        private String noticeMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySalarySettlementDto {
        private Long contractId;
        private Long flatId;
        private String billingMonth;
        private String staffMaskedName;
        private String serviceType;
        private int calendarDays;
        private int scheduledWorkingDays;
        private int presentDays;
        private int paidLeaveDays;
        private int unpaidAbsenceDays;
        private BigDecimal baseStipend;
        private BigDecimal proRataDeduction;
        private BigDecimal netPayable;
        private String paymentStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrossSocietyStaffProfileDto {
        private String staffUuid;
        private String displayName;
        private String primaryCategory;
        private String maskedPhone;
        private BigDecimal aggregateRating;
        private int totalReviews;
        private List<String> affiliatedCommunities;
    }
}
