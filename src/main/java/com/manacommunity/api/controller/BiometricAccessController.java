package com.manacommunity.api.controller;

import com.manacommunity.api.dto.AccessVerifyRequest;
import com.manacommunity.api.dto.TurnstileRequest;
import com.manacommunity.api.model.AccessLog;
import com.manacommunity.api.model.Turnstile;
import com.manacommunity.api.service.BiometricAccessService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
public class BiometricAccessController {

    private final BiometricAccessService biometricAccessService;
    private final LoggedInUserService loggedInUserService;

    // ── Turnstiles ───────────────────────────────────────────────────────

    @GetMapping("/turnstiles")
    public ResponseEntity<List<Turnstile>> getTurnstiles(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.getTurnstiles(communityId));
    }

    @GetMapping("/turnstiles/{id}")
    public ResponseEntity<Turnstile> getTurnstile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.getTurnstile(communityId, id));
    }

    @PostMapping("/turnstiles")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Turnstile> createTurnstile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TurnstileRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.createTurnstile(communityId, request));
    }

    @PutMapping("/turnstiles/{id}/status")
    public ResponseEntity<Turnstile> updateTurnstileStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam String status) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.updateTurnstileStatus(communityId, id, status));
    }

    // ── Access Logs ──────────────────────────────────────────────────────

    @GetMapping("/access-logs")
    public ResponseEntity<List<AccessLog>> getAccessLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long user,
            @RequestParam(required = false) Long turnstile,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        Long communityId = currentUser.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.getAccessLogs(communityId, user, turnstile, from, to));
    }

    @PostMapping("/access-logs/verify")
    public ResponseEntity<AccessLog> verifyAccess(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccessVerifyRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        Long communityId = currentUser.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.verifyAccess(communityId, request));
    }

    @GetMapping("/access-logs/summary")
    public ResponseEntity<Map<String, Object>> getAccessSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        Long communityId = currentUser.getCommunity().getId();
        return ResponseEntity.ok(biometricAccessService.getAccessSummary(communityId));
    }

    /**
     * Face biometric verification — called by turnstile hardware / guard tablet.
     * Mobile biometricAccessService.ts: POST /api/v1/access/turnstiles/verify-face
     */
    @PostMapping("/turnstiles/verify-face")
    public ResponseEntity<Map<String, Object>> verifyFace(
            @RequestBody Map<String, Object> request) {
        double confidence = request.get("confidenceScore") instanceof Number n
                ? n.doubleValue() : 0.0;
        double threshold = 0.80;
        boolean granted = confidence >= threshold;
        return ResponseEntity.ok(Map.of(
                "decision", granted ? "ACCESS_GRANTED" : "ACCESS_DENIED",
                "confidenceScore", confidence,
                "reason", granted
                        ? "Face verified with " + Math.round(confidence * 100) + "% confidence"
                        : "Confidence below threshold (< 80%)",
                "relayPulseDurationMs", granted ? 3000 : 0,
                "turnstileCode", request.getOrDefault("turnstileCode", "")
        ));
    }

    /**
     * Manual relay trigger — guard/admin unlocks a specific turnstile remotely.
     * Mobile biometricAccessService.ts: POST /api/v1/access/turnstiles/{id}/relay-unlock
     */
    @PostMapping("/turnstiles/{id}/relay-unlock")
    public ResponseEntity<Map<String, Object>> relayUnlock(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        int duration = body != null && body.get("durationSeconds") instanceof Number n
                ? n.intValue() : 3;
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Turnstile #" + id + " relay pulsed for " + duration + "s",
                "turnstileId", id
        ));
    }
}
