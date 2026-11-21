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

    @Transactional
    public KycVerificationRequest initiateAadhaarOtp(Long residentId, Long communityId, String aadhaarHash) {
        String externalRef = kycApiClient.initiateAadhaarOtp(aadhaarHash);
        KycVerificationRequest req = KycVerificationRequest.builder()
                .residentId(residentId)
                .communityId(communityId)
                .verificationMethod("AADHAAR_OTP")
                .idType("AADHAAR")
                .idNumberHash(aadhaarHash)
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
                                                          String documentUrl) {
        KycVerificationRequest req = KycVerificationRequest.builder()
                .residentId(residentId)
                .communityId(communityId)
                .verificationMethod("MANUAL_REVIEW")
                .idType(idType)
                .idNumberHash(idNumberHash)
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
