package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.dto.BiometricDtos.*;
import com.manacommunity.api.privacy.ConsentGuard;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/access/turnstiles")
@RequiredArgsConstructor
public class BiometricTurnstileController {

    private final BiometricTurnstileService turnstileService;
    private final ConsentGuard consentGuard;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BiometricEdgeSyncService edgeSyncService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AntiPassbackService antiPassbackService;

    @PostMapping("/verify-face")
    public ResponseEntity<VerifyFaceResult> verifyFace(@Valid @RequestBody VerifyFaceRequest request) {
        return ResponseEntity.ok(turnstileService.verifyFace(request));
    }

    @PostMapping("/kiosk-attendance")
    public ResponseEntity<KioskAttendanceResult> recordKioskAttendance(@Valid @RequestBody KioskAttendanceRequest request) {
        return ResponseEntity.ok(turnstileService.recordKioskAttendance(request));
    }

    @PostMapping("/sync-offline-punches")
    public ResponseEntity<OfflinePunchSyncResponse> syncOfflinePunches(@Valid @RequestBody OfflinePunchSyncRequest request) {
        return ResponseEntity.ok(turnstileService.syncOfflinePunches(request));
    }

    /**
     * DPDP Act 2023 s.6 — biometric data is sensitive personal data.
     * Explicit, separate consent for BIOMETRIC purpose must be granted before enrolment.
     * [LEGAL REVIEW REQUIRED] Confirm separate consent UI flow covers s.6(1) requirements.
     */
    @PostMapping("/enroll")
    public ResponseEntity<Void> enrollUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EnrollBiometricRequest request) {
        consentGuard.requireConsent(
            principal.getId(),
            "BIOMETRIC",
            "Biometric enrolment requires explicit consent. Please grant it in Privacy Centre → Privacy & Visibility."
        );
        turnstileService.enrollUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<List<BiometricTurnstileDto>> getTurnstiles(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long communityId) {
        requireSameCommunity(principal, communityId);
        return ResponseEntity.ok(turnstileService.getTurnstilesForCommunity(communityId));
    }

    @GetMapping("/logs/{communityId}")
    public ResponseEntity<Page<BiometricAccessLogDto>> getAccessLogs(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long communityId,
            Pageable pageable) {
        requireSameCommunity(principal, communityId);
        return ResponseEntity.ok(turnstileService.getAccessLogs(communityId, pageable));
    }

    @PatchMapping("/{turnstileId}/status")
    public ResponseEntity<Void> updateStatusPatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long turnstileId,
            @RequestParam BiometricEnums.TurnstileStatus status) {
        turnstileService.setTurnstileStatus(principal.getCommunityId(), turnstileId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{turnstileId}/status", params = "type=turnstile")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long turnstileId,
            @RequestParam BiometricEnums.TurnstileStatus status) {
        turnstileService.setTurnstileStatus(principal.getCommunityId(), turnstileId, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deltas/{communityId}")
    public ResponseEntity<List<BiometricEdgeDelta>> getEdgeDeltas(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "0") long sinceVersion) {
        requireSameCommunity(principal, communityId);
        if (edgeSyncService == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(edgeSyncService.getDeltasSince(communityId, sinceVersion));
    }

    @PostMapping("/revoke/{userId}")
    public ResponseEntity<java.util.Map<String, Object>> revokeBiometricKillSwitch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (edgeSyncService == null) return ResponseEntity.notFound().build();
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(edgeSyncService.revokeAndBroadcastKillSwitch(principal.getCommunityId(), userId, reason));
    }

    @PostMapping("/apb-swipe")
    public ResponseEntity<AntiPassbackService.ApbSwipeResult> evaluateApbSwipe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody java.util.Map<String, Object> body) {
        if (antiPassbackService == null) return ResponseEntity.notFound().build();
        Long communityId = body.containsKey("communityId")
                ? Long.valueOf(body.get("communityId").toString())
                : principal.getCommunityId();
        String zoneId = body.getOrDefault("zoneId", "CLUBHOUSE_GYM").toString();
        String credentialId = body.get("credentialId").toString();
        String direction = body.getOrDefault("direction", "ENTRY").toString();
        String turnstileId = body.getOrDefault("turnstileId", "TURNSTILE_1").toString();
        int irPersonsDetected = body.containsKey("irPersonsDetected")
                ? Integer.parseInt(body.get("irPersonsDetected").toString())
                : 1;
        return ResponseEntity.ok(antiPassbackService.evaluateSwipe(
                communityId, zoneId, credentialId, direction, turnstileId, irPersonsDetected, java.time.LocalDateTime.now()));
    }

    /** Tenant isolation: callers may only read data for their own community. */
    private static void requireSameCommunity(UserPrincipal principal, Long communityId) {
        if (principal == null || principal.getCommunityId() == null
                || !principal.getCommunityId().equals(communityId)) {
            throw new org.springframework.security.access.AccessDeniedException("Cross-community access denied");
        }
    }
}
