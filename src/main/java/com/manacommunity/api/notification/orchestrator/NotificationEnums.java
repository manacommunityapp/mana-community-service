package com.manacommunity.api.notification.orchestrator;

public class NotificationEnums {
    public enum NotificationChannel {
        IN_APP,
        PUSH,
        EMAIL,
        SMS,
        WHATSAPP
    }

    public enum NotificationCategory {
        EMERGENCY_SOS,
        GATE_ACCESS,
        FINANCIAL_BILLING,
        HELPDESK_TICKET,
        COMMUNITY_NOTICE,
        AMENITY_BOOKING,
        CHAT_MESSAGE,
        GENERAL
    }

    public enum NotificationPriority {
        CRITICAL,
        HIGH,
        NORMAL,
        LOW
    }

    public enum DeliveryStrategy {
        ALL_CHANNELS,
        PREFERENCE_BASED,
        FALLBACK_CASCADE
    }

    public enum DeliveryStatus {
        DELIVERED,
        SUPPRESSED_QUIET_HOURS,
        FAILED,
        OPTED_OUT,
        FALLBACK_TRIGGERED,
        QUEUED
    }
}
