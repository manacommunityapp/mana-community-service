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

    @PostMapping("/verify-face")
    public ResponseEntity<VerifyFaceResult> verifyFace(@Valid @RequestBody VerifyFaceRequest request) {
        return ResponseEntity.ok(turnstileService.verifyFace(request));
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
    public ResponseEntity<List<BiometricTurnstileDto>> getTurnstiles(@PathVariable Long communityId) {
        return ResponseEntity.ok(turnstileService.getTurnstilesForCommunity(communityId));
    }

    @GetMapping("/logs/{communityId}")
    public ResponseEntity<Page<BiometricAccessLogDto>> getAccessLogs(
            @PathVariable Long communityId,
            Pageable pageable) {
        return ResponseEntity.ok(turnstileService.getAccessLogs(communityId, pageable));
    }

    @PutMapping("/{turnstileId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long turnstileId,
            @RequestParam BiometricEnums.TurnstileStatus status) {
        turnstileService.setTurnstileStatus(turnstileId, status);
        return ResponseEntity.ok().build();
    }
}
