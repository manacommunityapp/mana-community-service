package com.manacommunity.api.access.biometric.engine;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.access.biometric.BiometricTurnstile;
import com.manacommunity.api.access.biometric.BiometricUserEnrollment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class TurnstileAccessRuleEngine {

    private static final BigDecimal MIN_MATCH_CONFIDENCE = new BigDecimal("0.80");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public RuleEvaluation evaluate(
            BiometricTurnstile turnstile,
            Optional<BiometricUserEnrollment> enrollmentOpt,
            BigDecimal confidenceScore,
            LocalDateTime checkTime) {

        // 1. Turnstile status check (Lockdown mode)
        if (turnstile.getStatus() == TurnstileStatus.LOCKDOWN) {
            // Only security guards may pass during active lockdown
            if (enrollmentOpt.isPresent() && enrollmentOpt.get().getPersonType() == BiometricPersonType.SECURITY_GUARD) {
                return new RuleEvaluation(AccessDecision.GRANTED_OPEN, true, "Emergency Security Bypass");
            }
            return new RuleEvaluation(AccessDecision.DENIED_LOCKDOWN, false, "Turnstile is in security lockdown");
        }

        // 2. Confidence check
        if (confidenceScore == null || confidenceScore.compareTo(MIN_MATCH_CONFIDENCE) < 0) {
            return new RuleEvaluation(AccessDecision.DENIED_UNENROLLED, false, "Low match confidence: " + confidenceScore);
        }

        // 3. Enrollment existence
        if (enrollmentOpt.isEmpty()) {
            return new RuleEvaluation(AccessDecision.DENIED_UNENROLLED, false, "No biometric enrollment matched");
        }

        BiometricUserEnrollment enrollment = enrollmentOpt.get();

        // 4. Enrollment status (revocation/expiry)
        if (enrollment.getEnrollmentStatus() == EnrollmentStatus.REVOKED) {
            return new RuleEvaluation(AccessDecision.DENIED_REVOKED, false, "Biometric enrollment revoked");
        }

        if (enrollment.getExpirationDate() != null && checkTime.isAfter(enrollment.getExpirationDate())) {
            return new RuleEvaluation(AccessDecision.DENIED_REVOKED, false, "Biometric pass expired on " + enrollment.getExpirationDate());
        }

        // 5. Domestic staff / Vendor allowed hours and day-of-week check
        if (enrollment.getPersonType() == BiometricPersonType.DOMESTIC_STAFF || enrollment.getPersonType() == BiometricPersonType.VENDOR_WORKER) {
            LocalTime time = checkTime.toLocalTime();
            if (!isWithinTimeWindow(time, enrollment.getTimeWindowStart(), enrollment.getTimeWindowEnd())) {
                return new RuleEvaluation(AccessDecision.DENIED_OUTSIDE_HOURS, false,
                        "Staff access not permitted at " + time + " (Allowed: " + enrollment.getTimeWindowStart() + " - " + enrollment.getTimeWindowEnd() + ")");
            }

            DayOfWeek day = checkTime.getDayOfWeek();
            if (!isDayAllowed(day, enrollment.getAllowedDays())) {
                return new RuleEvaluation(AccessDecision.DENIED_OUTSIDE_HOURS, false,
                        "Access not permitted on " + day.name());
            }
        }

        return new RuleEvaluation(AccessDecision.GRANTED_OPEN, true, "Access verified");
    }

    private boolean isWithinTimeWindow(LocalTime time, String startStr, String endStr) {
        if (startStr == null || endStr == null) return true;
        try {
            LocalTime start = LocalTime.parse(startStr, TIME_FORMATTER);
            LocalTime end = LocalTime.parse(endStr, TIME_FORMATTER);
            return !time.isBefore(start) && !time.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isDayAllowed(DayOfWeek day, String allowedDaysStr) {
        if (allowedDaysStr == null || allowedDaysStr.isBlank()) return true;
        String dayAbbr = day.name().substring(0, 3); // MON, TUE, etc.
        return allowedDaysStr.toUpperCase().contains(dayAbbr);
    }

    public record RuleEvaluation(AccessDecision decision, boolean unlockRelay, String reason) {}
}
