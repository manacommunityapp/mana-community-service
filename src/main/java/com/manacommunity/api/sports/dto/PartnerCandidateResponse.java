package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single candidate returned by the unified partner-search endpoint
 * (GET /api/sports/partner-search).
 *
 * <p>Covers both AppUser (flat owner / tenant / staff) and FamilyMember records
 * belonging to the same community. Each candidate includes pre-computed eligibility
 * against the target event's age and gender rules so the frontend can show helpful
 * inline badges without doing any extra round-trips.
 *
 * <p>Privacy note (DPDP): phone and email are intentionally omitted — they must
 * not be exposed to other community members. Only name, flat info, gender, and age
 * are surfaced.
 */
@Data
@Builder
public class PartnerCandidateResponse {

    /**
     * Discriminator — either "USER" (AppUser) or "FAMILY_MEMBER" (FamilyMember).
     */
    private String type;

    /**
     * Entity PK — AppUser.id or FamilyMember.id depending on {@link #type}.
     */
    private Long id;

    /**
     * For FAMILY_MEMBER candidates: the AppUser.id of the primary account holder.
     * Null for USER candidates.
     */
    private Long primaryUserId;

    /** Display name. */
    private String name;

    /**
     * Relation label for family members (Son, Daughter, Spouse, …).
     * Null for USER candidates.
     */
    private String relation;

    /**
     * Human-readable occupancy label, e.g. "Owner · Flat A-402" or "Family (Son) · Flat B-201".
     * Built server-side so the UI can display it as-is.
     */
    private String occupancyLabel;

    /** Raw flat number (e.g. "402"). Used for sorting / secondary display. */
    private String flatNo;

    /** Block identifier (e.g. "A"). */
    private String block;

    /** Normalised gender string ("MALE" | "FEMALE" | "OTHER"). */
    private String gender;

    /** Computed age (years). Null if neither dob nor age is available. */
    private Integer age;

    /** True when this candidate satisfies all age + gender constraints for the target event. */
    private boolean eligible;

    /**
     * Human-readable reason why the candidate is ineligible.
     * Null when {@link #eligible} is true.
     * Examples: "Age 16 — min 19", "Gender mismatch (need FEMALE)"
     */
    private String ineligibilityReason;
}
