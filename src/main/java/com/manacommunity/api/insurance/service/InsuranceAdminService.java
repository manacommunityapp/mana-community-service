package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.*;
import com.manacommunity.api.insurance.entity.InsuranceEnums.Category;
import com.manacommunity.api.insurance.entity.InsuranceEnums.PartnerStatus;
import com.manacommunity.api.insurance.entity.InsuranceEnums.VerificationStatus;
import com.manacommunity.api.insurance.entity.InsurancePartner;
import com.manacommunity.api.insurance.entity.InsuranceProduct;
import com.manacommunity.api.insurance.entity.InsuranceProductVersion;
import com.manacommunity.api.insurance.entity.PartnerWebhookEvent;
import com.manacommunity.api.insurance.partner.RedirectValidator;
import com.manacommunity.api.insurance.repository.*;
import com.manacommunity.api.user.model.AppUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Partner and product administration with maker-checker: the approver must be a different admin than the
 * creator/submitter. This is enforced here AND by DB CHECK constraints.
 */
@Service
@RequiredArgsConstructor
public class InsuranceAdminService {

    public record ProductVersionSubmitRequest(@NotNull Long partnerId, @NotNull Category category,
                                              @Size(max = 60) String subtype, @NotBlank @Size(max = 200) String name,
                                              @Size(max = 500) String brochureUrl, @Size(max = 500) String policyWordingUrl,
                                              String eligibilityJson, String coverageJson, String benefitsJson,
                                              String waitingPeriodsJson, String exclusionsJson, String networkJson,
                                              String disclosuresJson) {}

    private final InsurancePartnerRepository partners;
    private final InsuranceProductRepository products;
    private final InsuranceProductVersionRepository versions;
    private final PartnerWebhookEventRepository webhookEvents;
    private final InsuranceAuditService audit;

    @Transactional
    public PartnerView createPartner(AppUser admin, PartnerCreateRequest r) {
        InsuranceAccess.requireAdmin(admin);
        if (partners.findByCode(r.code()).isPresent()) {
            throw new ManaCommunityException("Partner code already exists", HttpStatus.CONFLICT, "DUPLICATE");
        }
        if (r.handoffBaseUrl() != null && !r.handoffBaseUrl().isBlank() && RedirectValidator.hostOf(r.handoffBaseUrl()) == null) {
            throw new ManaCommunityException("Handoff URL must be a valid https URL", HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }
        InsurancePartner p = partners.save(InsurancePartner.builder()
                .code(r.code()).legalName(r.legalName()).partnerType(r.partnerType())
                .irdaiRegistrationNo(r.irdaiRegistrationNo()).handoffBaseUrl(r.handoffBaseUrl())
                .disclosureText(r.disclosureText()).commercialDisclosure(r.commercialDisclosure())
                .createdBy(admin.getId()).build());
        audit.record(admin.getId(), "PARTNER_CREATED", "PARTNER", String.valueOf(p.getId()), "code=" + p.getCode());
        return view(p);
    }

    @Transactional
    public PartnerView approvePartner(AppUser admin, Long id, PartnerApproveRequest r) {
        InsuranceAccess.requireAdmin(admin);
        InsurancePartner p = partners.findById(id)
                .orElseThrow(() -> new ManaCommunityException("Partner not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        if (admin.getId().equals(p.getCreatedBy())) {
            throw new ManaCommunityException("Maker cannot approve their own change", HttpStatus.FORBIDDEN, "MAKER_CHECKER");
        }
        if (p.getIrdaiRegistrationNo() == null || p.getIrdaiRegistrationNo().isBlank()) {
            throw new ManaCommunityException("IRDAI registration number is required before approval",
                    HttpStatus.BAD_REQUEST, "AUTHORISATION_EVIDENCE_REQUIRED");
        }
        p.setApprovedBy(admin.getId());
        p.setApprovedAt(LocalDateTime.now());
        p.setAuthorisationVerified(true);
        p.setAuthorisationVerifiedAt(LocalDateTime.now());
        p.setStatus(PartnerStatus.ACTIVE);
        partners.save(p);
        audit.record(admin.getId(), "PARTNER_APPROVED", "PARTNER", String.valueOf(id), r.authorisationEvidenceNote());
        return view(p);
    }

    @Transactional
    public PartnerView suspendPartner(AppUser admin, Long id) {
        InsuranceAccess.requireAdmin(admin);
        InsurancePartner p = partners.findById(id)
                .orElseThrow(() -> new ManaCommunityException("Partner not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        p.setStatus(PartnerStatus.SUSPENDED);
        partners.save(p);
        audit.record(admin.getId(), "PARTNER_SUSPENDED", "PARTNER", String.valueOf(id), null);
        return view(p);
    }

    public List<PartnerView> listPartners(AppUser admin) {
        InsuranceAccess.requireAdmin(admin);
        return partners.findAll().stream().map(InsuranceAdminService::view).toList();
    }

    @Transactional
    public Long submitProductVersion(AppUser admin, ProductVersionSubmitRequest r) {
        InsuranceAccess.requireAdmin(admin);
        partners.findById(r.partnerId())
                .orElseThrow(() -> new ManaCommunityException("Partner not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        InsuranceProduct product = products.findByCategoryAndStatus(r.category(), "ACTIVE").stream()
                .filter(p -> p.getPartnerId().equals(r.partnerId()) && p.getName().equalsIgnoreCase(r.name()))
                .findFirst()
                .orElseGet(() -> products.save(InsuranceProduct.builder().partnerId(r.partnerId())
                        .category(r.category()).subtype(r.subtype()).name(r.name()).build()));
        InsuranceProductVersion v = versions.save(InsuranceProductVersion.builder()
                .productId(product.getId()).versionNo(versions.maxVersion(product.getId()) + 1)
                .dataSource("MANUAL_ENTRY").verificationStatus(VerificationStatus.PENDING_APPROVAL)
                .brochureUrl(r.brochureUrl()).policyWordingUrl(r.policyWordingUrl())
                .eligibilityJson(r.eligibilityJson()).coverageJson(r.coverageJson()).benefitsJson(r.benefitsJson())
                .waitingPeriodsJson(r.waitingPeriodsJson()).exclusionsJson(r.exclusionsJson())
                .networkJson(r.networkJson()).disclosuresJson(r.disclosuresJson()).submittedBy(admin.getId()).build());
        audit.record(admin.getId(), "PRODUCT_VERSION_SUBMITTED", "PRODUCT_VERSION", String.valueOf(v.getId()), null);
        return v.getId();
    }

    @Transactional
    public void approveProductVersion(AppUser admin, Long versionId) {
        InsuranceAccess.requireAdmin(admin);
        InsuranceProductVersion v = versions.findById(versionId)
                .orElseThrow(() -> new ManaCommunityException("Version not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        if (admin.getId().equals(v.getSubmittedBy())) {
            throw new ManaCommunityException("Maker cannot approve their own change", HttpStatus.FORBIDDEN, "MAKER_CHECKER");
        }
        if (v.getPolicyWordingUrl() == null || v.getPolicyWordingUrl().isBlank()) {
            throw new ManaCommunityException("Policy wording link is required before verification",
                    HttpStatus.BAD_REQUEST, "DISCLOSURE_REQUIRED");
        }
        v.setVerificationStatus(VerificationStatus.VERIFIED);
        v.setApprovedBy(admin.getId());
        v.setVerifiedAt(LocalDateTime.now());
        versions.save(v);
        audit.record(admin.getId(), "PRODUCT_VERSION_APPROVED", "PRODUCT_VERSION", String.valueOf(versionId), null);
    }

    public List<WebhookEventView> failedWebhooks(AppUser admin) {
        InsuranceAccess.requireAdmin(admin);
        return webhookEvents.findTop100ByOutcomeNotOrderByReceivedAtDesc("PROCESSED").stream()
                .map(InsuranceAdminService::view).toList();
    }

    static PartnerView view(InsurancePartner p) {
        return new PartnerView(p.getId(), p.getCode(), p.getLegalName(), p.getPartnerType(), p.getIrdaiRegistrationNo(),
                p.isAuthorisationVerified(), p.getStatus().name(), p.getHandoffBaseUrl(), p.getCreatedBy(), p.getApprovedBy());
    }

    static WebhookEventView view(PartnerWebhookEvent e) {
        return new WebhookEventView(e.getId(), e.getPartnerCode(), e.getEventId(), e.isSignatureValid(),
                e.getEventType(), e.getOutcome(), e.getReceivedAt());
    }
}
