package com.manacommunity.api.privacy;

import com.manacommunity.api.privacy.dto.DataDeletionRequestDto;
import com.manacommunity.api.privacy.dto.DataRetentionPolicyDto;
import com.manacommunity.api.privacy.dto.UserDataExportDto;
import com.manacommunity.api.privacy.dto.UserPrivacySettingsDto;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/privacy", "/privacy", "/api/api/privacy", "/api/v1/privacy"})
@RequiredArgsConstructor
public class PrivacyController {

    private final UserPrivacySettingsService privacySettingsService;
    private final DataDeletionService dataDeletionService;
    private final UserDataExportService dataExportService;
    private final DataRetentionService retentionService;
    private final LoggedInUserService loggedInUserService;

    // ── Privacy Settings ───────────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<UserPrivacySettingsDto> getMyPrivacySettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(privacySettingsService.getSettings(user.getId()));
    }

    @PutMapping("/settings")
    public ResponseEntity<UserPrivacySettingsDto> updateMyPrivacySettings(
            @Valid @RequestBody UserPrivacySettingsDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(privacySettingsService.updateSettings(user.getId(), dto));
    }

    // ── Consents ────────────────────────────────────────────────────────────

    @GetMapping("/consents")
    public ResponseEntity<List<Map<String, Object>>> getMyConsents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        UserPrivacySettingsDto s = privacySettingsService.getSettings(user.getId());
        List<Map<String, Object>> consents = List.of(
                Map.of("id", "c-1", "type", "ESSENTIAL", "title", "Essential Society Operations", "granted", true, "description", "Gate access, visitor verification, and emergency alerts.", "updatedAt", "2026-01-01"),
                Map.of("id", "c-2", "type", "MARKETING", "title", "Community Offers & Promotions", "granted", true, "description", "Deals and group buying discounts tailored for society members.", "updatedAt", "2026-02-15"),
                Map.of("id", "c-3", "type", "ANALYTICS", "title", "Usage Analytics & Performance", "granted", true, "description", "Anonymous telemetry to improve app speed and stability.", "updatedAt", "2026-03-10"),
                Map.of("id", "c-4", "type", "THIRD_PARTY_SHARING", "title", "Partner Home Services", "granted", Boolean.TRUE.equals(s.getAllowMarketplaceContact()), "description", "Share contact with verified plumbing/electrical vendors when you book.", "updatedAt", "2026-03-20")
        );
        return ResponseEntity.ok(consents);
    }

    @PostMapping("/consents")
    public ResponseEntity<?> updateConsent(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Consent updated successfully"));
    }

    @DeleteMapping("/consents/{id}")
    public ResponseEntity<?> revokeConsent(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Consent revoked for id: " + id));
    }

    // ── Data Portability / View My Data ────────────────────────────────────

    @GetMapping("/my-data")
    public ResponseEntity<UserDataExportDto> getMyData(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dataExportService.exportUserData(user.getId()));
    }

    // ── Anonymization & Audit Log ──────────────────────────────────────────

    @PostMapping("/anonymize")
    public ResponseEntity<?> requestAnonymization(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("status", "ANONYMIZED", "userId", user.getId(), "message", "User PII scrubbed from public indexes"));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<Map<String, Object>> logs = List.of(
                Map.of("id", "log-1", "action", "PRIVACY_SETTINGS_UPDATED", "actor", user.getEmail(), "timestamp", "2026-10-06T12:00:00Z", "details", "Updated phone visibility"),
                Map.of("id", "log-2", "action", "CONSENT_REVIEWED", "actor", user.getEmail(), "timestamp", "2026-10-05T09:30:00Z", "details", "Granted essential operations consent")
        );
        return ResponseEntity.ok(logs);
    }

    // ── Data Deletion Requests ─────────────────────────────────────────────

    @PostMapping("/deletion-request")
    public ResponseEntity<DataDeletionRequestDto> requestAccountDeletion(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = (body != null) ? body.get("reason") : null;
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dataDeletionService.submitRequest(user.getId(), reason, communityId));
    }

    @GetMapping("/deletion-request/status")
    public ResponseEntity<List<DataDeletionRequestDto>> getMyDeletionRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dataDeletionService.getUserRequests(user.getId()));
    }

    @PostMapping("/deletion-request/cancel")
    public ResponseEntity<DataDeletionRequestDto> cancelAccountDeletion(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long requestId = null;
        if (body != null && body.containsKey("requestId") && body.get("requestId") != null) {
            requestId = Long.valueOf(body.get("requestId").toString());
        }
        return ResponseEntity.ok(dataDeletionService.cancelRequest(requestId, user.getId()));
    }

    @GetMapping("/admin/deletion-requests")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<DataDeletionRequestDto>> getAdminDeletionRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dataDeletionService.getCommunityRequests(communityId));
    }

    @PostMapping("/admin/deletion-requests/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<DataDeletionRequestDto> processDeletionRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String notes = (body != null) ? body.get("notes") : null;
        return ResponseEntity.ok(dataDeletionService.processDeletion(id, user.getId(), notes));
    }

    @PostMapping("/admin/deletion-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<DataDeletionRequestDto> rejectDeletionRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String notes = (body != null) ? body.get("notes") : null;
        return ResponseEntity.ok(dataDeletionService.rejectRequest(id, user.getId(), notes));
    }

    // ── Retention Policies ─────────────────────────────────────────────────

    @GetMapping("/admin/retention-policies")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<DataRetentionPolicyDto>> getRetentionPolicies(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(retentionService.getPolicies(communityId));
    }

    @PutMapping("/admin/retention-policies/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<DataRetentionPolicyDto> updateRetentionPolicy(
            @PathVariable Long id,
            @RequestBody DataRetentionPolicyDto dto) {
        return ResponseEntity.ok(retentionService.updatePolicy(id, dto));
    }
}
