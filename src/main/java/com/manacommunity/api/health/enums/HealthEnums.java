package com.manacommunity.api.health.enums;

public class HealthEnums {
    public enum ProviderCategory {
        DOCTOR, CLINIC, DENTIST, DIAGNOSTIC_CENTER, PHARMACY, PHYSIOTHERAPIST, HOME_CARE
    }
    public enum ConsultationMode {
        IN_PERSON, VIDEO, PHONE, HOME_VISIT
    }
    public enum AppointmentStatus {
        REQUESTED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW
    }
    public enum VerificationStatus {
        SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED
    }
    public enum FamilyRelationship {
        SELF, SPOUSE, CHILD, PARENT, DEPENDENT
    }
    public enum RecordType {
        PRESCRIPTION, LAB_REPORT, VACCINATION, ALLERGY, CONDITION, DISCHARGE_SUMMARY
    }
    public enum CampCategory {
        DIABETES_CARDIAC, DENTAL_EYE, VACCINATION, KIDS_NUTRITION, SENIOR_SCREENING, GENERAL_HEALTH
    }
}
