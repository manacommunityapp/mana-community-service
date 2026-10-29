package com.manacommunity.api.mobile.kyc;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KYC (Know Your Customer) document verification for residents.
 */
@RestController
@RequestMapping("/api/kyc")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class KycController {

    private final ResidentKycRepository kycRepo;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/kyc/status - returns KycSummary with overall status and document list */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<ResidentKycDocument> docs = kycRepo.findByUserIdOrderBySubmittedAtDesc(user.getId());

        String overallStatus = computeOverallStatus(docs);
        String submittedAt = docs.isEmpty() ? null : docs.get(0).getSubmittedAt().toString();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overallStatus", overallStatus);
        summary.put("submittedAt", submittedAt);
        summary.put("documents", docs.stream().map(this::toDocResponse).collect(Collectors.toList()));
        return ResponseEntity.ok(summary);
    }

    /** POST /api/kyc/submit - submit a new KYC document */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Community community = user.getCommunity();
        if (community == null) {
            return ResponseEntity.badRequest().build();
        }
        String docType = body.getOrDefault("documentType", "OTHER");
        String docNumber = body.getOrDefault("documentNumber", "");

        ResidentKycDocument doc = ResidentKycDocument.builder()
                .user(user)
                .community(community)
                .documentType(docType)
                .documentNumber(docNumber)
                .status("PENDING_REVIEW")
                .frontImageUrl(body.get("frontImageUrl"))
                .backImageUrl(body.get("backImageUrl"))
                .submittedAt(LocalDateTime.now())
                .build();

        doc = kycRepo.save(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDocResponse(doc));
    }

    /** PUT /api/kyc/documents/{id} - resubmit/update a document */
    @PutMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> resubmitDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AppUser user = loggedInUserService.resolve(principal);
        ResidentKycDocument doc = kycRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KycDocument", id));
        if (!doc.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You do not own this document.");
        }
        if (body.containsKey("documentNumber")) doc.setDocumentNumber(body.get("documentNumber"));
        if (body.containsKey("frontImageUrl"))  doc.setFrontImageUrl(body.get("frontImageUrl"));
        if (body.containsKey("backImageUrl"))   doc.setBackImageUrl(body.get("backImageUrl"));
        doc.setStatus("PENDING_REVIEW");
        doc.setRejectionReason(null);
        doc.setSubmittedAt(LocalDateTime.now());
        doc = kycRepo.save(doc);
        return ResponseEntity.ok(toDocResponse(doc));
    }

    private String computeOverallStatus(List<ResidentKycDocument> docs) {
        if (docs.isEmpty()) return "NOT_SUBMITTED";
        if (docs.stream().anyMatch(d -> "APPROVED".equals(d.getStatus()))) return "APPROVED";
        if (docs.stream().anyMatch(d -> "REJECTED".equals(d.getStatus()))) return "REJECTED";
        if (docs.stream().anyMatch(d -> "PENDING_REVIEW".equals(d.getStatus()))) return "PENDING_REVIEW";
        return "NOT_SUBMITTED";
    }

    private Map<String, Object> toDocResponse(ResidentKycDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              d.getId());
        m.put("documentType",    d.getDocumentType());
        m.put("documentNumber",  d.getDocumentNumber());
        m.put("status",          d.getStatus());
        m.put("submittedAt",     d.getSubmittedAt() != null ? d.getSubmittedAt().toString() : null);
        m.put("reviewedAt",      d.getReviewedAt() != null ? d.getReviewedAt().toString() : null);
        m.put("rejectionReason", d.getRejectionReason());
        m.put("frontImageUrl",   d.getFrontImageUrl());
        m.put("backImageUrl",    d.getBackImageUrl());
        return m;
    }
}
