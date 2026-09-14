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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/privacy")
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

    // ── Data Portability / View My Data ────────────────────────────────────

    @GetMapping("/my-data")
    public ResponseEntity<UserDataExportDto> getMyData(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dataExportService.exportUserData(user.getId()));
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
