package com.manacommunity.api.access.biometric;

public class BiometricEnums {
    public enum TurnstileDirection {
        ENTRY,
        EXIT,
        BIDIRECTIONAL
    }

    public enum TurnstileStatus {
        ONLINE,
        OFFLINE,
        MAINTENANCE,
        LOCKDOWN
    }

    public enum BiometricPersonType {
        RESIDENT,
        DOMESTIC_STAFF,
        VENDOR_WORKER,
        SECURITY_GUARD
    }

    public enum EnrollmentStatus {
        ENROLLED,
        PENDING_PHOTO,
        REVOKED,
        EXPIRED
    }

    public enum AccessDecision {
        GRANTED_OPEN,
        DENIED_UNENROLLED,
        DENIED_OUTSIDE_HOURS,
        DENIED_REVOKED,
        DENIED_LOCKDOWN
    }
}
