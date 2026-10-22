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
}
