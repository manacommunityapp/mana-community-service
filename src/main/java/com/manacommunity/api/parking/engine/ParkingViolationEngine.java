package com.manacommunity.api.parking.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ParkingViolationEngine {

    private static final BigDecimal DEFAULT_HOURLY_PENALTY = new BigDecimal("100.00");
    private static final BigDecimal BASE_VIOLATION_FEE = new BigDecimal("250.00");

    public enum ViolationSeverity {
        NONE, MINOR_OVERSTAY, MAJOR_OVERSTAY, UNAUTHORIZED_SLOT_OCCUPATION
    }

    public record ViolationAssessment(boolean isViolation, ViolationSeverity severity, BigDecimal penaltyAmount, String remarks) {}

    /**
     * Assesses visitor parking validity and computes overstay penalty if pass has expired.
     */
    public ViolationAssessment assessVisitorPass(LocalDateTime validUntil, LocalDateTime currentTime, BigDecimal customHourlyRate) {
        if (validUntil == null || currentTime == null || !currentTime.isAfter(validUntil)) {
            return new ViolationAssessment(false, ViolationSeverity.NONE, BigDecimal.ZERO, "Pass is valid");
        }

        Duration overstay = Duration.between(validUntil, currentTime);
        long hours = Math.max(1, (overstay.toMinutes() + 59) / 60);

        BigDecimal hourlyRate = (customHourlyRate != null && customHourlyRate.compareTo(BigDecimal.ZERO) > 0)
                ? customHourlyRate : DEFAULT_HOURLY_PENALTY;

        BigDecimal penalty = BASE_VIOLATION_FEE.add(hourlyRate.multiply(BigDecimal.valueOf(hours)));
        ViolationSeverity severity = hours > 4 ? ViolationSeverity.MAJOR_OVERSTAY : ViolationSeverity.MINOR_OVERSTAY;

        return new ViolationAssessment(
                true,
                severity,
                penalty.setScale(2, RoundingMode.HALF_UP),
                "Visitor overstayed by " + hours + " hour(s)."
        );
    }

    /**
     * Assesses unauthorized parking in reserved/assigned slot.
     */
    public ViolationAssessment assessUnauthorizedSlotOccupation(Long assignedUserId, Long actualUserId, String slotNumber) {
        if (assignedUserId == null || assignedUserId.equals(actualUserId)) {
            return new ViolationAssessment(false, ViolationSeverity.NONE, BigDecimal.ZERO, "Authorized slot usage");
        }
        return new ViolationAssessment(
                true,
                ViolationSeverity.UNAUTHORIZED_SLOT_OCCUPATION,
                new BigDecimal("500.00"),
                "Unauthorized vehicle parked in assigned slot " + slotNumber
        );
    }
}
