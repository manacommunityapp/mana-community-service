package com.manacommunity.api.kyc.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.kyc.dto.KycSubmitRequest;
import com.manacommunity.api.kyc.dto.KycSummaryResponse;
import com.manacommunity.api.kyc.entity.KycDocument;
import com.manacommunity.api.kyc.repository.KycDocumentRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KycService {

    /** JSON field names the client must send in place of the retired raw number. */
    private static final String EXPECTED_FIELDS = "'idNumberHash' and 'idNumberLast4'";

    private final KycDocumentRepository repo;
    private final KycIdNumberProtector idNumberProtector;
    private final KycDocumentImageStore imageStore;

    @Transactional(readOnly = true)
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

    @Transactional
    public KycDocument submit(KycSubmitRequest req, AppUser user) {
        validate(req);

        // The images are stored BEFORE the row is built, and a storage failure throws out of
        // here and rolls the transaction back. The bug this replaces was the opposite: the
        // base64 fields were bound and read by nothing, so every submission persisted a hash
        // with front_image_url / back_image_url NULL and the admin review screen had no
        // evidence to review. A KYC record without its document is worse than a failed
        // submission, so do not "recover" from a storage error by saving the row anyway.
        if (req.getFrontImageBase64() == null || req.getFrontImageBase64().isBlank()) {
            throw new InvalidInputException("'frontImageBase64' is required: a KYC document cannot be "
                    + "reviewed without an image of the document front.");
        }
        String frontUrl = imageStore.store(req.getFrontImageBase64(), "frontImageBase64",
                req.getDocumentType(), user.getId(), "front");
        String backUrl = imageStore.store(req.getBackImageBase64(), "backImageBase64",
                req.getDocumentType(), user.getId(), "back");

        KycDocument doc = KycDocument.builder()
                .user(user)
                .community(user.getCommunity())
                .documentType(req.getDocumentType())
                // The client's SHA-256 is a transport control only: Aadhaar is 12 digits, so all
                // 10^12 plain SHA-256 digests can be precomputed offline in milliseconds and a
                // database dump would hand over every ID number. Re-protect with
                // HMAC-SHA256(server pepper, client digest) before it touches a row, and fail
                // closed (503) when no pepper is configured. Do NOT "simplify" this to store
                // req.getIdNumberHash() directly — see KycIdNumberProtector.
                .idNumberHash(idNumberProtector.protectForStorage(req.getIdNumberHash(), "idNumberHash"))
                .idNumberLast4(idNumberProtector.requireLast4(req.getIdNumberLast4(), "idNumberLast4"))
                .frontImageUrl(frontUrl)
                .backImageUrl(backUrl)
                .status("PENDING_REVIEW")
                .build();
        return repo.save(doc);
    }

    @Transactional
    public KycDocument resubmit(Long id, KycSubmitRequest req, AppUser user) {
        validate(req);

        KycDocument doc = repo.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.getUser().getId().equals(user.getId())) throw new RuntimeException("Not authorized");

        // Same reasoning as submit(): only the peppered HMAC is ever written to the column.
        doc.setIdNumberHash(idNumberProtector.protectForStorage(req.getIdNumberHash(), "idNumberHash"));
        doc.setIdNumberLast4(idNumberProtector.requireLast4(req.getIdNumberLast4(), "idNumberLast4"));
        if (req.getDocumentType() != null && !req.getDocumentType().isBlank()) {
            doc.setDocumentType(req.getDocumentType());
        }

        // Images are optional on a resubmission: a resident correcting only the number keeps
        // the images already on file. When new ones are sent they must store successfully
        // before anything is written, same as submit(). The previously stored file is left in
        // place rather than deleted — the old row's URL is the only audit trail of what an
        // earlier review looked at.
        String documentTypeForPath = doc.getDocumentType();
        String newFrontUrl = imageStore.store(req.getFrontImageBase64(), "frontImageBase64",
                documentTypeForPath, user.getId(), "front");
        String newBackUrl = imageStore.store(req.getBackImageBase64(), "backImageBase64",
                documentTypeForPath, user.getId(), "back");
        if (newFrontUrl != null) doc.setFrontImageUrl(newFrontUrl);
        if (newBackUrl != null) doc.setBackImageUrl(newBackUrl);
        doc.setStatus("PENDING_REVIEW");
        doc.setRejectionReason(null);
        return repo.save(doc);
    }

    /**
     * Rejects an old client that still transmits the raw number, then checks the shape of
     * the replacement fields. Both produce 400 before anything is persisted.
     */
    private void validate(KycSubmitRequest req) {
        if (req == null) {
            throw new InvalidInputException("Request body is required.");
        }
        idNumberProtector.rejectRawIdNumber(req.getDocumentNumber(), "documentNumber", EXPECTED_FIELDS);
        if (req.getDocumentType() == null || req.getDocumentType().isBlank()) {
            throw new InvalidInputException("'documentType' is required.");
        }
    }
}
