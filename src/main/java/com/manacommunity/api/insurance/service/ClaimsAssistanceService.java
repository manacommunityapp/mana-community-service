package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.*;
import com.manacommunity.api.insurance.entity.ClaimsAssistanceCase;
import com.manacommunity.api.insurance.entity.InsuranceEnums.CaseStatus;
import com.manacommunity.api.insurance.entity.InsuranceEnums.ConsentPurpose;
import com.manacommunity.api.insurance.repository.ClaimsAssistanceCaseRepository;
import com.manacommunity.api.insurance.repository.InsurancePolicyRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mana claims-ASSISTANCE tickets. These are not insurer claims: no claim decision field exists, and
 * insurerClaimStatus is always null until an authorised partner integration supplies one.
 */
@Service
@RequiredArgsConstructor
public class ClaimsAssistanceService {

    public static final String NOTICE =
            "This is a Mana assistance request, not a claim filed with your insurer. Mana cannot approve, "
          + "guarantee or settle claims. File and track the claim with your insurer.";

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final ClaimsAssistanceCaseRepository cases;
    private final InsurancePolicyRepository policies;
    private final InsuranceConsentService consents;
    private final InsuranceAuditService audit;

    @Transactional
    public ClaimCaseView create(AppUser user, ClaimCaseRequest r) {
        if (!consents.isActive(user.getId(), ConsentPurpose.CLAIMS_ASSISTANCE, null)) {
            throw new ManaCommunityException("Consent for claims assistance is required", HttpStatus.FORBIDDEN, "CONSENT_REQUIRED");
        }
        if (r.policyId() != null) {
            policies.findByIdAndUserId(r.policyId(), user.getId())
                    .orElseThrow(() -> new ManaCommunityException("Policy not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        }
        ClaimsAssistanceCase c = cases.save(ClaimsAssistanceCase.builder()
                .caseRef("MAC-" + token(8)).userId(user.getId())
                .communityId(user.getCommunity() == null ? null : user.getCommunity().getId())
                .policyId(r.policyId()).claimType(r.claimType().trim()).description(r.description())
                .assignedTeam("Insurance Support").build());
        audit.record(user.getId(), "CLAIM_CASE_CREATED", "CLAIM_CASE", String.valueOf(c.getId()), "type=" + c.getClaimType());
        return view(c);
    }

    public List<ClaimCaseView> list(Long userId) {
        return cases.findByUserIdOrderByCreatedAtDesc(userId).stream().map(ClaimsAssistanceService::view).toList();
    }

    public ClaimCaseView get(Long userId, Long id) {
        return view(owned(userId, id));
    }

    /** User records the reference number the INSURER gave them. Stored as user-declared; never as a status. */
    @Transactional
    public ClaimCaseView attachInsurerReference(Long userId, Long id, String reference) {
        ClaimsAssistanceCase c = owned(userId, id);
        c.setInsurerClaimReference(reference.trim());
        c.setInsurerClaimReferenceSource("USER_DECLARED");
        c.setUpdatedAt(LocalDateTime.now());
        cases.save(c);
        audit.record(userId, "CLAIM_CASE_INSURER_REF_ADDED", "CLAIM_CASE", String.valueOf(id), null);
        return view(c);
    }

    @Transactional
    public ClaimCaseView adminUpdate(AppUser admin, Long id, CaseUpdateRequest r) {
        InsuranceAccess.requireAdmin(admin);
        ClaimsAssistanceCase c = cases.findById(id)
                .orElseThrow(() -> new ManaCommunityException("Case not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        CaseStatus before = c.getStatus();
        if (r.status() != null) c.setStatus(r.status());
        if (r.assignedTeam() != null) c.setAssignedTeam(r.assignedTeam());
        c.setUpdatedAt(LocalDateTime.now());
        cases.save(c);
        audit.record(admin.getId(), "CLAIM_CASE_UPDATED", "CLAIM_CASE", String.valueOf(id), before + "->" + c.getStatus());
        return view(c);
    }

    private ClaimsAssistanceCase owned(Long userId, Long id) {
        return cases.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ManaCommunityException("Case not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }

    private String token(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    public static ClaimCaseView view(ClaimsAssistanceCase c) {
        return new ClaimCaseView(c.getId(), c.getCaseRef(), c.getPolicyId(), c.getClaimType(), c.getDescription(),
                c.getStatus().name(), c.getAssignedTeam(), c.getInsurerClaimReference(),
                c.getInsurerClaimReferenceSource(), null, c.getCreatedAt(), NOTICE);
    }
}
