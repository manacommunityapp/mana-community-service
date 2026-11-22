package com.manacommunity.api.kyc.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ServiceDegradedException;
import com.manacommunity.api.kyc.dto.KycSubmitRequest;
import com.manacommunity.api.kyc.dto.KycSummaryResponse;
import com.manacommunity.api.kyc.entity.KycDocument;
import com.manacommunity.api.kyc.repository.KycDocumentRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository repo;
    private final KycIdNumberProtector idProtector;

    public KycSummaryResponse getStatus(AppUser user) {
        List<KycDocument> docs = repo.findByUserId(user.getId());
        String overall = "NOT_SUBMITTED";
        if (!docs.isEmpty()) {
            boolean anyApproved = docs.stream().anyMatch(d -> "APPROVED".equals(d.getStatus()));
            boolean anyPending = docs.stream().anyMatch(d -> "PENDING_REVIEW".equals(d.getStatus()));
            boolean anyRejected = docs.stream().anyMatch(d -> "REJECTED".equals(d.getStatus()));
            if (anyApproved) overall = "APPROVED";
            else if (anyPending) overall = "PENDING_REVIEW";
            else if (anyRejected) overall = "REJECTED";
        }
        return KycSummaryResponse.builder().overallStatus(overall).documents(docs).build();
    }

    public KycDocument submit(KycSubmitRequest req, AppUser user) {
        // Reject raw ID numbers — the client must send a SHA-256 hash
        if (req.hasRawDocumentNumber()) {
            log.warn("SECURITY: KYC submit rejected — raw documentNumber supplied by user {}", user.getId());
            throw new InvalidInputException("Raw government ID numbers must not be transmitted. "
                    + "Send govtIdNumberHash (SHA-256 of the normalised number) instead.");
        }
        if (!idProtector.isConfigured()) {
            log.error("SECURITY: KYC submit rejected — MANA_KYC_ID_PEPPER not configured");
            throw new ServiceDegradedException("KYC service is temporarily unavailable. Contact support.");
        }

        String pepperedHash = null;
        if (req.getGovtIdNumberHash() != null && !req.getGovtIdNumberHash().isBlank()) {
            pepperedHash = idProtector.protectForStorage(req.getGovtIdNumberHash(), "govtIdNumberHash");
        }

        KycDocument doc = KycDocument.builder()
                .user(user)
                .community(user.getCommunity())
                .documentType(req.getDocumentType())
                .idNumberHash(pepperedHash)
                .idNumberLast4(req.getGovtIdLast4())
                .status("PENDING_REVIEW")
                .build();
        return repo.save(doc);
    }

    public KycDocument resubmit(Long id, KycSubmitRequest req, AppUser user) {
        if (req.hasRawDocumentNumber()) {
            log.warn("SECURITY: KYC resubmit rejected — raw documentNumber supplied by user {}", user.getId());
            throw new InvalidInputException("Raw government ID numbers must not be transmitted. "
                    + "Send govtIdNumberHash (SHA-256 of the normalised number) instead.");
        }

        KycDocument doc = repo.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.getUser().getId().equals(user.getId())) throw new RuntimeException("Not authorized");

        if (req.getGovtIdNumberHash() != null && !req.getGovtIdNumberHash().isBlank()) {
            if (!idProtector.isConfigured()) {
                throw new ServiceDegradedException("KYC service is temporarily unavailable. Contact support.");
            }
            doc.setIdNumberHash(idProtector.protectForStorage(req.getGovtIdNumberHash(), "govtIdNumberHash"));
            doc.setIdNumberLast4(req.getGovtIdLast4());
        }
        doc.setStatus("PENDING_REVIEW");
        doc.setRejectionReason(null);
        return repo.save(doc);
    }
}
