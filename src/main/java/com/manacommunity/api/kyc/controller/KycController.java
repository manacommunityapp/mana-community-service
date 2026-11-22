package com.manacommunity.api.kyc.controller;

import com.manacommunity.api.kyc.dto.*;
import com.manacommunity.api.kyc.entity.KycDocument;
import com.manacommunity.api.kyc.entity.KycVerificationRequest;
import com.manacommunity.api.kyc.service.KycIdNumberProtector;
import com.manacommunity.api.kyc.service.KycService;
import com.manacommunity.api.kyc.service.KycVerificationService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycVerificationService kycService;
    private final KycService kycDocumentService;
    private final KycIdNumberProtector idNumberProtector;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<KycVerificationRequest>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return kycService.getKycStatus(user.getId())
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null, "No KYC submission found")));
    }

    @PostMapping("/aadhaar/initiate")
    public ResponseEntity<ApiResponse<KycVerificationRequest>> initiateAadhaar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AadhaarInitiateRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        idNumberProtector.rejectRawIdNumber(req.getAadhaarNumber(), "aadhaarNumber", "'aadhaarHash'");
        KycVerificationRequest result = kycService.initiateAadhaarOtp(
                user.getId(), req.getCommunityId(), req.getAadhaarHash(), req.getAadhaarLast4());
        return ResponseEntity.ok(ApiResponse.ok(result, "OTP sent to registered Aadhaar mobile"));
    }

    @PostMapping("/aadhaar/verify")
    public ResponseEntity<ApiResponse<KycVerificationRequest>> verifyAadhaar(
            @RequestBody AadhaarVerifyRequest req) {
        KycVerificationRequest result = kycService.verifyAadhaarOtp(req.getRequestId(), req.getOtp());
        String message = "VERIFIED".equals(result.getStatus()) ? "KYC verified" : "OTP verification failed";
        return ResponseEntity.ok(ApiResponse.ok(result, message));
    }

    @PostMapping("/document")
    public ResponseEntity<ApiResponse<KycVerificationRequest>> submitDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DocumentSubmitRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        idNumberProtector.rejectRawIdNumber(req.getIdNumber(), "idNumber",
                "'idNumberHash' and 'idNumberLast4'");
        KycVerificationRequest result = kycService.submitDocumentForReview(
                user.getId(), req.getCommunityId(), req.getIdType(),
                req.getIdNumberHash(), req.getIdNumberLast4(), req.getDocumentUrl());
        return ResponseEntity.ok(ApiResponse.ok(result, "Document submitted for review"));
    }

    // ── Document submission (mobile onboarding) ───────────────────────────
    // The client sends idNumberHash + idNumberLast4; the raw number is never
    // transmitted and a request that still carries documentNumber is refused with 400.

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<KycSummaryResponse>> listDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ApiResponse.ok(kycDocumentService.getStatus(user)));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<KycDocument>> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody KycSubmitRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        KycDocument doc = kycDocumentService.submit(req, user);
        return ResponseEntity.ok(ApiResponse.ok(doc, "KYC document submitted for review"));
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<KycDocument>> resubmitDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody KycSubmitRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        KycDocument doc = kycDocumentService.resubmit(id, req, user);
        return ResponseEntity.ok(ApiResponse.ok(doc, "KYC document resubmitted for review"));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<KycVerificationRequest>>> getPending(@AuthenticationPrincipal UserPrincipal principal
) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(ApiResponse.ok(kycService.getPendingReviews(communityId)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<KycVerificationRequest>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        KycVerificationRequest result = kycService.approveManualKyc(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(result, "KYC approved"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<KycVerificationRequest>> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody KycRejectRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        KycVerificationRequest result = kycService.rejectKyc(id, user.getId(), req.getReason());
        return ResponseEntity.ok(ApiResponse.ok(result, "KYC rejected"));
    }
}
