package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.CaregiverConsent;
import com.manacommunity.api.healthcare.service.CaregiverConsentService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/healthcare/caregiver-consents")
@RequiredArgsConstructor
public class CaregiverConsentController {

    private final CaregiverConsentService consentService;

    @PostMapping
    public ResponseEntity<CaregiverConsent> grant(
            @RequestBody CaregiverConsent consent,
            @AuthenticationPrincipal UserPrincipal principal) {
        consent.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(consentService.grantConsent(consent));
    }

    @PutMapping("/{id}/revoke")
    public ResponseEntity<CaregiverConsent> revoke(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consentService.revokeConsent(id, principal.getUserId()));
    }

    @GetMapping("/patient/{patientUserId}")
    public ResponseEntity<List<CaregiverConsent>> forPatient(
            @PathVariable Long patientUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consentService.getActiveConsentsForPatient(principal.getCommunityId(), patientUserId));
    }

    @GetMapping("/caregiver")
    public ResponseEntity<List<CaregiverConsent>> forMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consentService.getActiveConsentsForCaregiver(principal.getCommunityId(), principal.getUserId()));
    }
}
