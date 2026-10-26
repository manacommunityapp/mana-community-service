package com.manacommunity.api.insurance.entity;

/** Status/category vocabularies shared across the insurance hub. Stored as strings. */
public final class InsuranceEnums {
    private InsuranceEnums() {}

    public enum Category {
        HEALTH, FAMILY_FLOATER, PARENTS, LIFE_TERM, MOTOR_CAR, MOTOR_TWO_WHEELER,
        HOME, TRAVEL, PERSONAL_ACCIDENT, OTHER
    }

    public enum PartnerStatus { PENDING_APPROVAL, ACTIVE, SUSPENDED }

    public enum VerificationStatus { UNVERIFIED, PENDING_APPROVAL, VERIFIED, REJECTED }

    /** PARTNER = real insurer/intermediary response; SANDBOX = labelled mock, never shown as a real quote. */
    public enum QuoteSource { PARTNER, SANDBOX }

    public enum PolicySource { USER_DECLARED, PARTNER_VERIFIED }

    public enum PolicyStatus { ACTIVE_DECLARED, ACTIVE_VERIFIED, EXPIRED, LAPSED, CANCELLED }

    public enum ConsentPurpose { QUOTE_SHARING, POLICY_STORAGE, CLAIMS_ASSISTANCE, GROUP_ENQUIRY, MARKETING }

    public enum ConsentEvent { GRANTED, WITHDRAWN }

    public enum QuoteRequestStatus { SUBMITTED, QUOTED, PARTNER_UNAVAILABLE, FAILED, CANCELLED }

    /** Mana-side assistance case status. Deliberately has no "claim approved/settled" state. */
    public enum CaseStatus { OPEN, IN_REVIEW, AWAITING_USER, CLOSED }

    public enum EnquiryStatus { SUBMITTED, ASSIGNED, PARTNER_REFERRED, CLOSED }

    public enum CoverageScope { COMMON_PROPERTY, INDIVIDUAL_RESIDENT, GROUP_POLICY }
}
