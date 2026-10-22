package com.manacommunity.api.healthguard.enums;

public class HealthGuardEnums {

    public enum IncidentSeverity {
        LEVEL_1_NOTICE,
        LEVEL_2_ATTENTION,
        LEVEL_3_POSSIBLE_EMERGENCY,
        LEVEL_4_CONFIRMED
    }

    public enum IncidentStatus {
        ACTIVE, ACKNOWLEDGED, RESOLVED, CANCELLED, ESCALATED
    }

    public enum AlertTriggerType {
        MANUAL_SOS, SYMPTOM_REPORT, HEART_RATE_ALERT, FALL_DETECTION, INACTIVITY_ALERT
    }

    public enum NotificationChannel {
        PUSH, SMS, IN_APP, PHONE_CALL
    }

    public enum NotificationStatus {
        PENDING, SENT, DELIVERED, ACKNOWLEDGED, FAILED
    }

    public enum ResponderStatus {
        AVAILABLE, ASSIGNED, EN_ROUTE, ARRIVED, COMPLETED, DECLINED
    }

    public enum BloodGroup {
        A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE,
        O_POSITIVE, O_NEGATIVE, AB_POSITIVE, AB_NEGATIVE, UNKNOWN
    }

    public enum ContactRelationship {
        SPOUSE, PARENT, CHILD, SIBLING, FRIEND, NEIGHBOR, DOCTOR, CAREGIVER, OTHER
    }

    public enum ResponderAvailability {
        AVAILABLE, BUSY, OFFLINE
    }

    public enum AmbulanceBookingStatus {
        REQUESTED, CONFIRMED, DISPATCHED, ARRIVED, COMPLETED, CANCELLED
    }

    public enum WearableType {
        APPLE_WATCH, FITBIT, GARMIN, SAMSUNG_GALAXY_WATCH, GENERIC_BLUETOOTH, MANUAL
    }

    public enum WearableEventType {
        FALL_DETECTED,
        HIGH_HEART_RATE,
        LOW_HEART_RATE,
        IRREGULAR_HEART_RATE,
        LOW_SPO2,
        INACTIVITY_ALERT,
        LOW_BATTERY,
        DEVICE_DISCONNECTED,
        MANUAL_CHECK_IN
    }
}
