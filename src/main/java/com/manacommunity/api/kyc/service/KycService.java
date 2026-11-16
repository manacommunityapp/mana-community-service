package com.manacommunity.api.kyc.service;

import com.manacommunity.api.kyc.dto.KycSubmitRequest;
import com.manacommunity.api.kyc.dto.KycSummaryResponse;
import com.manacommunity.api.kyc.entity.KycDocument;
import com.manacommunity.api.kyc.repository.KycDocumentRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository repo;

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
        KycDocument doc = KycDocument.builder()
                .user(user)
                .community(user.getCommunity())
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .status("PENDING_REVIEW")
                .build();
        return repo.save(doc);
    }

    public KycDocument resubmit(Long id, KycSubmitRequest req, AppUser user) {
        KycDocument doc = repo.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.getUser().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        doc.setDocumentNumber(req.getDocumentNumber());
        doc.setStatus("PENDING_REVIEW");
        doc.setRejectionReason(null);
        return repo.save(doc);
    }
}
