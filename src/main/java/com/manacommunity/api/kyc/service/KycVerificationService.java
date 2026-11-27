package com.manacommunity.api.kyc.service;

import com.manacommunity.api.kyc.entity.KycVerificationRequest;
import com.manacommunity.api.kyc.repository.KycVerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycVerificationService {

    private final KycVerificationRequestRepository repo;
    private final KycApiClient kycApiClient;
    private final KycIdNumberProtector idNumberProtector;

    @Transactional
    public KycVerificationRequest initiateAadhaarOtp(Long residentId, Long communityId,
                                                     String aadhaarHash, String aadhaarLast4) {
        // The authority is given the client digest it expects; only the stored copy is peppered.
        String clientDigest = idNumberProtector.requireClientDigest(aadhaarHash, "aadhaarHash");
        String externalRef = kycApiClient.initiateAadhaarOtp(clientDigest);
        KycVerificationRequest req = KycVerificationRequest.builder()
                .residentId(residentId)
                .communityId(communityId)
                .verificationMethod("AADHAAR_OTP")
                .idType("AADHAAR")
                // The client's bare SHA-256 must never be the stored value: a 12-digit Aadhaar
                // has only 10^12 candidates, so a dump of plain digests is a dump of the numbers.
                // Store HMAC-SHA256(server pepper, client digest) instead, and fail closed with
                // 503 when no pepper is configured. See KycIdNumberProtector.
                .idNumberHash(idNumberProtector.protectForStorage(clientDigest, "aadhaarHash"))
                .idNumberLast4(aadhaarLast4 == null || aadhaarLast4.isBlank()
                        ? null
                        : idNumberProtector.requireLast4(aadhaarLast4, "aadhaarLast4"))
                .status("OTP_SENT")
                .externalRef(externalRef)
                .build();
        return repo.save(req);
    }

    @Transactional
    public KycVerificationRequest verifyAadhaarOtp(Long requestId, String otp) {
        KycVerificationRequest req = repo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("KYC request not found: " + requestId));

        boolean accepted = kycApiClient.verifyAadhaarOtp(req.getExternalRef(), otp);
        if (accepted) {
            req.setStatus("VERIFIED");
            req.setVerifiedAt(Instant.now());
            req.setExpiresAt(Instant.now().plus(730, ChronoUnit.DAYS));
            syncVerifiedFlag(req.getResidentId());
        } else {
            req.setStatus("REJECTED");
            req.setRejectionReason("OTP mismatch");
        }
        return repo.save(req);
    }

    @Transactional
    public KycVerificationRequest submitDocumentForReview(Long residentId, Long communityId,
                                                          String idType, String idNumberHash,
                                                          String idNumberLast4, String documentUrl) {
        KycVerificationRequest req = KycVerificationRequest.builder()
                .residentId(residentId)
                .communityId(communityId)
                .verificationMethod("MANUAL_REVIEW")
                .idType(idType)
                // Same reasoning as initiateAadhaarOtp(): the client digest alone is offline
                // enumerable, so only the peppered HMAC is written. See KycIdNumberProtector.
                .idNumberHash(idNumberProtector.protectForStorage(idNumberHash, "idNumberHash"))
                // Optional here: this endpoint predates the masked-display change and older
                // clients do not send it. Validated when present.
                .idNumberLast4(idNumberLast4 == null || idNumberLast4.isBlank()
                        ? null
                        : idNumberProtector.requireLast4(idNumberLast4, "idNumberLast4"))
                .documentUrl(documentUrl)
                .status("PENDING")
                .build();
        return repo.save(req);
    }

    @Transactional
    public KycVerificationRequest approveManualKyc(Long requestId, Long approvedBy) {
        KycVerificationRequest req = repo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("KYC request not found: " + requestId));
        req.setStatus("VERIFIED");
        req.setVerifiedBy(approvedBy);
        req.setVerifiedAt(Instant.now());
        req.setExpiresAt(Instant.now().plus(730, ChronoUnit.DAYS));
        syncVerifiedFlag(req.getResidentId());
        return repo.save(req);
    }

    @Transactional
    public KycVerificationRequest rejectKyc(Long requestId, Long rejectedBy, String reason) {
        KycVerificationRequest req = repo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("KYC request not found: " + requestId));
        req.setStatus("REJECTED");
        req.setVerifiedBy(rejectedBy);
        req.setRejectionReason(reason);
        return repo.save(req);
    }

    @Transactional(readOnly = true)
    public Optional<KycVerificationRequest> getKycStatus(Long residentId) {
        return repo.findTopByResidentIdOrderByCreatedAtDesc(residentId);
    }

    @Transactional(readOnly = true)
    public List<KycVerificationRequest> getPendingReviews(Long communityId) {
        return repo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, "PENDING");
    }

    private void syncVerifiedFlag(Long residentId) {
        // Placeholder: update id_verified on household_members once that entity is linked
        log.info("KYC verified for residentId={} — household flag sync pending FK wiring", residentId);
    }
}
