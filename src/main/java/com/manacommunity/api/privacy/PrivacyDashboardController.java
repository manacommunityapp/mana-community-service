package com.manacommunity.api.privacy;

import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.platform.privacy.ConsentPurposeCode;
import com.manacommunity.platform.privacy.ConsentService;
import com.manacommunity.platform.privacy.UserConsentRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
@Tag(name = "Privacy", description = "DPDP Act compliance — consent management and data rights")
public class PrivacyDashboardController {

    private final ConsentService consentService;

    @GetMapping("/consent")
    @Operation(summary = "Get the authenticated user's full consent history")
    public ResponseEntity<ApiResponse<List<UserConsentRecord>>> getConsentHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<UserConsentRecord> history = consentService.getConsentHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/consent/{purpose}/status")
    @Operation(summary = "Check active consent status for a specific purpose")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConsentStatus(
            @PathVariable ConsentPurposeCode purpose,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean active = consentService.hasActiveConsent(principal.getId(), purpose);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "purpose", purpose,
                "active", active
        )));
    }

    @PostMapping("/consent/{purpose}/grant")
    @Operation(summary = "Grant consent for a specific purpose (DPDP §6)")
    public ResponseEntity<ApiResponse<UserConsentRecord>> grantConsent(
            @PathVariable ConsentPurposeCode purpose,
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserConsentRecord record = consentService.grantConsent(
                principal.getId(), communityId, purpose, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(record,
                "Consent granted for " + purpose));
    }

    @PostMapping("/consent/{purpose}/revoke")
    @Operation(summary = "Revoke consent for a specific purpose (DPDP §6)")
    public ResponseEntity<ApiResponse<UserConsentRecord>> revokeConsent(
            @PathVariable ConsentPurposeCode purpose,
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserConsentRecord record = consentService.revokeConsent(
                principal.getId(), communityId, purpose, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(record,
                "Consent revoked for " + purpose));
    }
}
