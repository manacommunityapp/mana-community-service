package com.manacommunity.api.safety.controller;

import com.manacommunity.api.safety.dto.TrustAndSafetyDtos.*;
import com.manacommunity.api.safety.model.LocationConsent;
import com.manacommunity.api.safety.service.TrustAndSafetyService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.permissions.AdminPermissions.VIEW_ADMIN;
import static com.manacommunity.api.constants.permissions.VisitorPermissions.VIEW_VISITORS;

@RestController
@RequestMapping("/api/safety/trust")
@RequiredArgsConstructor
public class TrustAndSafetyController {

    private final TrustAndSafetyService trustAndSafetyService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/overview")
    public ResponseEntity<TrustSafetyOverviewDto> getOverview(
            @RequestParam(defaultValue = "1") Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN, VIEW_VISITORS);
        return ResponseEntity.ok(trustAndSafetyService.getOverview(communityId));
    }

    @PostMapping("/memberships/{userId}/verify")
    public ResponseEntity<VerifiedMembershipDto> verifyMembership(
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        KycStatus status = KycStatus.valueOf(payload.getOrDefault("status", "VERIFIED"));
        String justification = payload.getOrDefault("justification", "Verified by Administrator");
        String admin = principal != null ? principal.getUsername() : "admin";
        return ResponseEntity.ok(trustAndSafetyService.updateMembershipKyc(userId, status, admin, justification));
    }

    @PostMapping("/location-consent/verify-access")
    public ResponseEntity<LocationAccessVerificationResponse> verifyLocationAccess(
            @RequestBody LocationAccessVerificationRequest request) {
        return ResponseEntity.ok(trustAndSafetyService.verifyLocationAccess(request));
    }

    @PostMapping("/location-consent")
    public ResponseEntity<LocationConsent> setLocationConsent(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        Long userId = principal != null ? principal.getId() : Long.valueOf(payload.get("userId").toString());
        Long communityId = payload.containsKey("communityId") ? Long.valueOf(payload.get("communityId").toString()) : 1L;
        LocationConsentPurpose purpose = LocationConsentPurpose.valueOf(payload.get("purpose").toString());
        boolean granted = Boolean.parseBoolean(payload.get("granted").toString());
        String clientIp = request.getRemoteAddr();

        return ResponseEntity.ok(trustAndSafetyService.setLocationConsent(userId, communityId, purpose, granted, clientIp));
    }

    @GetMapping("/location-consent")
    public ResponseEntity<List<LocationConsent>> getUserLocationConsents(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long targetUserId = (userId != null) ? userId : (principal != null ? principal.getId() : 101L);
        return ResponseEntity.ok(trustAndSafetyService.getUserConsents(targetUserId));
    }
}