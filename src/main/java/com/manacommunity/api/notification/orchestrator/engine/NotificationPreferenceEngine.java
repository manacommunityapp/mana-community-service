package com.manacommunity.api.notification.orchestrator.engine;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.NotificationPreference;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NotificationPreferenceEngine {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Determines whether a notification can be delivered on a given channel considering user preferences,
     * category, priority, and quiet hours.
     *
     * Rules:
     * 1. CRITICAL priority (Emergency/SOS) ALWAYS bypasses quiet hours and explicit opt-outs.
     * 2. IN_APP channel is never blocked by quiet hours (in-box delivery is non-intrusive).
     * 3. If explicit preference exists and is disabled -> OPTED_OUT.
     * 4. If quiet hours enabled and current time falls within window -> SUPPRESSED_QUIET_HOURS.
     * 5. Otherwise -> DELIVERABLE.
     */
    public DeliveryDecision evaluate(
            NotificationCategory category,
            NotificationChannel channel,
            NotificationPriority priority,
            Optional<NotificationPreference> userPreference,
            LocalTime currentTime) {

        // Rule 1: Emergency & Critical bypass
        if (priority == NotificationPriority.CRITICAL || category == NotificationCategory.EMERGENCY_SOS) {
            return new DeliveryDecision(true, DeliveryStatus.QUEUED, "Emergency priority bypasses quiet hours and preferences");
        }

        // Rule 2: Explicit preference check
        if (userPreference.isPresent()) {
            NotificationPreference pref = userPreference.get();
            if (Boolean.FALSE.equals(pref.getIsEnabled())) {
                return new DeliveryDecision(false, DeliveryStatus.OPTED_OUT, "User has disabled " + channel + " for " + category);
            }

            // Rule 3: Quiet Hours check
            if (Boolean.TRUE.equals(pref.getQuietHoursEnabled()) && channel != NotificationChannel.IN_APP) {
                if (isInsideQuietHours(currentTime, pref.getQuietHoursStart(), pref.getQuietHoursEnd())) {
                    return new DeliveryDecision(false, DeliveryStatus.SUPPRESSED_QUIET_HOURS, 
                            "Suppressed due to active quiet hours (" + pref.getQuietHoursStart() + " - " + pref.getQuietHoursEnd() + ")");
                }
            }
        }

        return new DeliveryDecision(true, DeliveryStatus.QUEUED, "Allowed by policy");
    }

    /**
     * Evaluates if a time is within quiet hours, handling overnight intervals (e.g., 22:00 to 07:00).
     */
    public boolean isInsideQuietHours(LocalTime time, String startStr, String endStr) {
        if (startStr == null || endStr == null || time == null) {
            return false;
        }
        try {
            LocalTime start = LocalTime.parse(startStr, TIME_FORMAT);
            LocalTime end = LocalTime.parse(endStr, TIME_FORMAT);

            if (start.isBefore(end)) {
                // Same-day window (e.g., 14:00 to 16:00)
                return !time.isBefore(start) && time.isBefore(end);
            } else {
                // Overnight window (e.g., 22:00 to 07:00)
                return !time.isBefore(start) || time.isBefore(end);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public record DeliveryDecision(boolean allowed, DeliveryStatus status, String reason) {}
}
