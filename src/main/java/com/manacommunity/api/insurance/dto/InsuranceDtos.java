package com.manacommunity.api.insurance.dto;

import com.manacommunity.api.insurance.entity.InsuranceEnums;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Request/response contracts for the insurance hub. See docs/insurance/api-spec.yaml. */
public final class InsuranceDtos {
    private InsuranceDtos() {}

    // ---- Catalogue
    public record CatalogueItem(Long productVersionId, Long productId, String category, String subtype, String name,
                                int versionNo, String insurerName, String partnerType, String irdaiRegistrationNo,
                                String dataSource, String brochureUrl, String policyWordingUrl,
                                String eligibilityJson, String coverageJson, String benefitsJson,
                                String waitingPeriodsJson, String exclusionsJson, String networkJson,
                                String disclosuresJson, String partnerDisclosure, String commercialDisclosure,
                                LocalDateTime verifiedAt) {}

    // ---- Consent
    public record ConsentGrantRequest(@NotNull InsuranceEnums.ConsentPurpose purpose, Long partnerId,
                                      @NotBlank @Size(max = 20) String noticeVersion) {}

    public record ConsentView(Long id, String purpose, Long partnerId, String noticeVersion, String eventType,
                              LocalDateTime createdAt) {}

    // ---- Quotes
    public record QuoteSubmitRequest(@NotNull InsuranceEnums.Category category,
                                     @NotBlank @Size(max = 40) String partnerCode,
                                     @NotNull Long consentId,
                                     @NotNull Map<String, Object> fields) {}

    public record QuoteView(Long id, String partnerName, String dataSource, BigDecimal premiumAmount,
                            String premiumFrequency, BigDecimal sumInsured, LocalDateTime quotedAt,
                            LocalDateTime validUntil, boolean expired, String label) {}

    public record QuoteRequestView(Long id, String category, String status, String failureCode, boolean replayed,
                                   List<QuoteView> quotes, String notice) {}

    // ---- Policies
    public record PolicyCreateRequest(@NotBlank @Size(max = 200) String insurerName,
                                      @NotBlank @Size(max = 200) String productName,
                                      @NotNull InsuranceEnums.Category category,
                                      @Size(max = 40) String policyNumber,
                                      LocalDate startDate, LocalDate expiryDate,
                                      @DecimalMin("0") BigDecimal sumInsured,
                                      @DecimalMin("0") BigDecimal premiumAmount,
                                      @Size(max = 20) String premiumFrequency,
                                      @Size(max = 120) String nomineeName,
                                      @Size(max = 300) String insurerContact) {}

    public record PolicyView(Long id, String insurerName, String productName, String category,
                             String maskedPolicyNumber, String dataSource, String status,
                             LocalDate startDate, LocalDate expiryDate, BigDecimal sumInsured,
                             BigDecimal premiumAmount, String premiumFrequency, String nomineeName,
                             String insurerContact, boolean coverageConfirmedByInsurer, String statusNote) {}

    // ---- Claims assistance (Mana tickets, not insurer claims)
    public record ClaimCaseRequest(Long policyId, @NotBlank @Size(max = 30) String claimType,
                                   @Size(max = 1000) String description) {}

    public record InsurerClaimRefRequest(@NotBlank @Size(max = 80) String reference) {}

    public record ClaimCaseView(Long id, String caseRef, Long policyId, String claimType, String description,
                                String assistanceStatus, String assignedTeam, String insurerClaimReference,
                                String insurerClaimReferenceSource, String insurerClaimStatus, LocalDateTime createdAt,
                                String notice) {}

    // ---- Group enquiries
    public record GroupEnquiryRequest(@NotBlank @Size(max = 200) String organisationName,
                                      @NotBlank @Size(max = 30) String organisationType,
                                      @NotNull InsuranceEnums.CoverageScope coverageScope,
                                      @NotBlank @Size(max = 40) String coverageSought,
                                      @NotNull @Min(2) @Max(1_000_000) Integer approxMembers,
                                      @Size(max = 1000) String requirements,
                                      @NotBlank @Pattern(regexp = "PHONE|EMAIL") String contactMethod,
                                      @NotNull Long consentId) {}

    public record GroupEnquiryView(Long id, String enquiryRef, String organisationName, String organisationType,
                                   String coverageScope, String coverageSought, Integer approxMembers,
                                   String status, LocalDateTime createdAt, String notice) {}

    // ---- Admin
    public record PartnerCreateRequest(@NotBlank @Pattern(regexp = "[A-Z0-9_]{2,40}") String code,
                                       @NotBlank @Size(max = 200) String legalName,
                                       @NotBlank @Pattern(regexp = "INSURER|IRDAI_INTERMEDIARY|REFERRAL_PARTNER") String partnerType,
                                       @Size(max = 60) String irdaiRegistrationNo,
                                       @Size(max = 500) String handoffBaseUrl,
                                       @Size(max = 2000) String disclosureText,
                                       @Size(max = 2000) String commercialDisclosure) {}

    public record PartnerApproveRequest(@NotBlank @Size(max = 300) String authorisationEvidenceNote) {}

    public record PartnerView(Long id, String code, String legalName, String partnerType, String irdaiRegistrationNo,
                              boolean authorisationVerified, String status, String handoffBaseUrl,
                              Long createdBy, Long approvedBy) {}

    public record CaseUpdateRequest(InsuranceEnums.CaseStatus status, @Size(max = 60) String assignedTeam) {}

    public record WebhookEventView(Long id, String partnerCode, String eventId, boolean signatureValid,
                                   String eventType, String outcome, LocalDateTime receivedAt) {}
}
