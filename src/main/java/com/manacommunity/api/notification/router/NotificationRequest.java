package com.manacommunity.api.notification.router;

import java.util.Map;

public record NotificationRequest(
        Long recipientId,
        String recipientPhone,
        String recipientEmail,
        String urgency,
        String title,
        String body,
        String category,
        Map<String, String> data,
        boolean dndOverride
) {
    public boolean isUrgent() { return "CRITICAL".equals(urgency) || "HIGH".equals(urgency); }
    public boolean isMedium() { return "MEDIUM".equals(urgency); }
}
