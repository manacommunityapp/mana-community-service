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
    public enum CareJourneyStage {
        APPOINTMENT_BOOKED, CONSULTATION_COMPLETED, PRESCRIPTION_ISSUED,
        LAB_TEST_ORDERED, RESULTS_READY, FOLLOW_UP_SCHEDULED
    }
    public enum MedicineOrderStatus {
        PLACED, PROCESSING, DISPATCHED, DELIVERED, CANCELLED
    }
    public enum LabCollectionStatus {
        BOOKED, PHLEBOTOMIST_ASSIGNED, SAMPLE_COLLECTED, PROCESSING, REPORT_READY, CANCELLED
    }
    public enum AmbulanceVehicleType {
        BASIC, ADVANCED, ICU
    }
    public enum AmbulanceStatus {
        REQUESTED, DISPATCHED, EN_ROUTE, ARRIVED, COMPLETED, CANCELLED
    }
    public enum EmergencyCallbackStatus {
        REQUESTED, AGENT_ASSIGNED, CALLED, RESOLVED, MISSED
    }
    public enum IpdAdmissionStatus {
        REQUESTED, CONFIRMED, ADMITTED, DISCHARGED, CANCELLED
    }
    public enum WardType {
        GENERAL, SEMI_PRIVATE, PRIVATE, ICU
    }
    public enum RefillFrequency {
        WEEKLY, MONTHLY, BI_MONTHLY
    }
    public enum AppointmentReminderOffset {
        MIN_15, MIN_30, HOUR_1, HOUR_2, DAY_1
    }
}
