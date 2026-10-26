package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.ConsentGrantRequest;
import com.manacommunity.api.insurance.dto.InsuranceDtos.ConsentView;
import com.manacommunity.api.insurance.entity.ConsentRecord;
import com.manacommunity.api.insurance.entity.InsuranceEnums.ConsentEvent;
import com.manacommunity.api.insurance.entity.InsuranceEnums.ConsentPurpose;
import com.manacommunity.api.insurance.repository.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Purpose-specific, withdrawable consent. Ledger is append-only; latest event wins. */
@Service
@RequiredArgsConstructor
public class InsuranceConsentService {

    private final ConsentRecordRepository repo;
    private final InsuranceAuditService audit;

    @Transactional
    public ConsentView grant(Long userId, ConsentGrantRequest req) {
        ConsentRecord saved = repo.save(ConsentRecord.builder()
                .userId(userId).purpose(req.purpose()).partnerId(req.partnerId())
                .noticeVersion(req.noticeVersion()).eventType(ConsentEvent.GRANTED).build());
        audit.record(userId, "CONSENT_GRANTED", "CONSENT", String.valueOf(saved.getId()), "purpose=" + req.purpose());
        return view(saved);
    }

    @Transactional
    public ConsentView withdraw(Long userId, Long consentId) {
        ConsentRecord granted = repo.findById(consentId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ManaCommunityException("Consent not found", HttpStatus.NOT_FOUND, "CONSENT_NOT_FOUND"));
        ConsentRecord saved = repo.save(ConsentRecord.builder()
                .userId(userId).purpose(granted.getPurpose()).partnerId(granted.getPartnerId())
                .noticeVersion(granted.getNoticeVersion()).eventType(ConsentEvent.WITHDRAWN)
                .grantedRefId(granted.getId()).build());
        audit.record(userId, "CONSENT_WITHDRAWN", "CONSENT", String.valueOf(saved.getId()), "purpose=" + granted.getPurpose());
        return view(saved);
    }

    public List<ConsentView> list(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(InsuranceConsentService::view).toList();
    }

    public boolean isActive(Long userId, ConsentPurpose purpose, Long partnerId) {
        Optional<ConsentRecord> latest = partnerId == null
                ? repo.findFirstByUserIdAndPurposeAndPartnerIdIsNullOrderByIdDesc(userId, purpose)
                : repo.findFirstByUserIdAndPurposeAndPartnerIdOrderByIdDesc(userId, purpose, partnerId);
        return latest.map(c -> c.getEventType() == ConsentEvent.GRANTED).orElse(false);
    }

    /** Validates that the referenced consent row belongs to the user, matches purpose, and has not been withdrawn. */
    public ConsentRecord requireActive(Long userId, Long consentId, ConsentPurpose purpose) {
        ConsentRecord c = repo.findById(consentId)
                .filter(r -> r.getUserId().equals(userId) && r.getPurpose() == purpose
                        && r.getEventType() == ConsentEvent.GRANTED)
                .orElseThrow(() -> consentRequired());
        if (!isActive(userId, purpose, c.getPartnerId())) {
            throw consentRequired();
        }
        return c;
    }

    private static ManaCommunityException consentRequired() {
        return new ManaCommunityException("Active consent for this purpose is required",
                HttpStatus.FORBIDDEN, "CONSENT_REQUIRED");
    }

    static ConsentView view(ConsentRecord c) {
        return new ConsentView(c.getId(), c.getPurpose().name(), c.getPartnerId(), c.getNoticeVersion(),
                c.getEventType().name(), c.getCreatedAt());
    }
}
