package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.PolicyCreateRequest;
import com.manacommunity.api.insurance.dto.InsuranceDtos.PolicyView;
import com.manacommunity.api.insurance.entity.InsurancePolicy;
import com.manacommunity.api.insurance.entity.InsuranceEnums.ConsentPurpose;
import com.manacommunity.api.insurance.entity.InsuranceEnums.PolicySource;
import com.manacommunity.api.insurance.entity.InsuranceEnums.PolicyStatus;
import com.manacommunity.api.insurance.entity.PolicyStatusHistory;
import com.manacommunity.api.insurance.repository.InsurancePolicyRepository;
import com.manacommunity.api.insurance.repository.PolicyStatusHistoryRepository;
import com.manacommunity.api.service.FieldEncryptionService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** My Policies. Every read/write is scoped to the owning user; other users' ids return 404 (no existence leak). */
@Service
@RequiredArgsConstructor
public class InsurancePolicyService {

    private final InsurancePolicyRepository policies;
    private final PolicyStatusHistoryRepository history;
    private final InsuranceConsentService consents;
    private final InsuranceAuditService audit;
    private final FieldEncryptionService encryption;

    @Transactional
    public PolicyView create(AppUser user, PolicyCreateRequest r) {
        if (!consents.isActive(user.getId(), ConsentPurpose.POLICY_STORAGE, null)) {
            throw new ManaCommunityException("Consent to store policy details is required", HttpStatus.FORBIDDEN, "CONSENT_REQUIRED");
        }
        if (r.startDate() != null && r.expiryDate() != null && r.expiryDate().isBefore(r.startDate())) {
            throw new ManaCommunityException("Expiry date must be on or after start date", HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }
        String digits = r.policyNumber() == null ? "" : r.policyNumber().replaceAll("[^A-Za-z0-9]", "");
        String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : null;
        String enc = null;
        if (!digits.isEmpty() && encryption.isEnabled()) {
            enc = encryption.encrypt(digits); // full number stored only when encryption is available
        }
        InsurancePolicy p = policies.save(InsurancePolicy.builder()
                .userId(user.getId()).communityId(user.getCommunity() == null ? null : user.getCommunity().getId())
                .insurerName(r.insurerName().trim()).productName(r.productName().trim()).category(r.category())
                .policyNumberLast4(last4).policyNumberEnc(enc)
                .dataSource(PolicySource.USER_DECLARED).status(PolicyStatus.ACTIVE_DECLARED)
                .startDate(r.startDate()).expiryDate(r.expiryDate()).sumInsured(r.sumInsured())
                .premiumAmount(r.premiumAmount()).premiumFrequency(r.premiumFrequency())
                .nomineeName(r.nomineeName()).insurerContact(r.insurerContact()).build());
        history.save(PolicyStatusHistory.builder().policyId(p.getId()).toStatus(p.getStatus().name())
                .source("USER").eventId("create-" + p.getId()).build());
        audit.record(user.getId(), "POLICY_CREATED", "POLICY", String.valueOf(p.getId()), "category=" + p.getCategory());
        return view(p, LocalDate.now());
    }

    public List<PolicyView> list(Long userId) {
        LocalDate today = LocalDate.now();
        return policies.findByUserIdOrderByExpiryDateAsc(userId).stream().map(p -> view(p, today)).toList();
    }

    public PolicyView get(Long userId, Long id) {
        return view(owned(userId, id), LocalDate.now());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        InsurancePolicy p = owned(userId, id);
        policies.delete(p);
        audit.record(userId, "POLICY_DELETED", "POLICY", String.valueOf(id), null);
    }

    /**
     * Partner-reported status change. Idempotent on (policyId, eventId): replays are no-ops.
     * Only a partner event can move a policy to a VERIFIED state.
     */
    @Transactional
    public boolean applyPartnerStatus(Long policyId, Long partnerId, String eventId, PolicyStatus newStatus) {
        InsurancePolicy p = policies.findById(policyId)
                .filter(x -> partnerId.equals(x.getPartnerId())).orElse(null);
        if (p == null) return false;
        if (history.existsByPolicyIdAndEventId(policyId, eventId)) return false;
        String from = p.getStatus().name();
        history.save(PolicyStatusHistory.builder().policyId(policyId).fromStatus(from)
                .toStatus(newStatus.name()).source("PARTNER_WEBHOOK").eventId(eventId).build());
        p.setStatus(newStatus);
        p.setDataSource(PolicySource.PARTNER_VERIFIED);
        p.setUpdatedAt(LocalDateTime.now());
        policies.save(p);
        audit.record(null, "POLICY_STATUS_SYNCED", "POLICY", String.valueOf(policyId), from + "->" + newStatus);
        return true;
    }

    private InsurancePolicy owned(Long userId, Long id) {
        return policies.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ManaCommunityException("Policy not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }

    static PolicyView view(InsurancePolicy p, LocalDate today) {
        boolean confirmed = p.getDataSource() == PolicySource.PARTNER_VERIFIED;
        PolicyStatus status = p.getStatus();
        // Date-derived display only; never asserts coverage is active.
        if (!confirmed && p.getExpiryDate() != null && p.getExpiryDate().isBefore(today)) status = PolicyStatus.EXPIRED;
        String note = confirmed
                ? "Status reported by the insurer/partner."
                : "Details were entered by you. Mana has not verified that this policy is active - confirm with your insurer.";
        return new PolicyView(p.getId(), p.getInsurerName(), p.getProductName(), p.getCategory().name(),
                mask(p.getPolicyNumberLast4()), p.getDataSource().name(), status.name(), p.getStartDate(),
                p.getExpiryDate(), p.getSumInsured(), p.getPremiumAmount(), p.getPremiumFrequency(),
                p.getNomineeName(), p.getInsurerContact(), confirmed, note);
    }

    public static String mask(String last4) {
        return last4 == null ? null : "XXXXXX" + last4;
    }
}
