package com.manacommunity.api.privacy;

import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyNoticeController {

    private static final String CURRENT_VERSION = "1.0";

    private final PrivacyNoticeAcceptanceRepository acceptanceRepository;

    /**
     * POST /api/privacy/accept
     * Resident records their acceptance of the current privacy notice.
     */
    @PostMapping("/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PrivacyNoticeAcceptance>> acceptPrivacyNotice(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        Long residentId   = principal.getId();
        Long communityId  = principal.getCommunityId();
        String ip         = resolveIp(request);
        String userAgent  = request.getHeader("User-Agent");

        if (acceptanceRepository.existsByResidentIdAndNoticeVersion(residentId, CURRENT_VERSION)) {
            PrivacyNoticeAcceptance existing = acceptanceRepository
                    .findByResidentIdAndNoticeVersion(residentId, CURRENT_VERSION)
                    .orElseThrow();
            return ResponseEntity.ok(ApiResponse.ok(existing, "Already accepted"));
        }

        PrivacyNoticeAcceptance acceptance = PrivacyNoticeAcceptance.builder()
                .residentId(residentId)
                .communityId(communityId)
                .noticeVersion(CURRENT_VERSION)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();

        PrivacyNoticeAcceptance saved = acceptanceRepository.save(acceptance);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Privacy notice accepted"));
    }

    /**
     * GET /api/privacy/my-acceptance
     * Resident checks whether they have accepted the current notice version.
     */
    @GetMapping("/my-acceptance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAcceptance(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long residentId = principal.getId();
        boolean accepted = acceptanceRepository
                .existsByResidentIdAndNoticeVersion(residentId, CURRENT_VERSION);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "accepted",       accepted,
                "noticeVersion",  CURRENT_VERSION
        )));
    }

    /**
     * GET /api/privacy/community/{communityId}/pending
     * ADMIN: list resident IDs who have not yet accepted the current notice version.
     */
    @GetMapping("/community/{communityId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Long>>> getPendingResidents(
            @PathVariable Long communityId) {

        List<Long> pending = acceptanceRepository
                .findResidentIdsWithoutAcceptance(communityId, CURRENT_VERSION);
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
