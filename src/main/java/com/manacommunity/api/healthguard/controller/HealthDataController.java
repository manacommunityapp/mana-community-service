package com.manacommunity.api.healthguard.controller;

import com.manacommunity.api.healthguard.dto.HealthDataConsentDto;
import com.manacommunity.api.healthguard.dto.HealthDataConsentRequest;
import com.manacommunity.api.healthguard.dto.HealthObservationBatchRequest;
import com.manacommunity.api.healthguard.dto.HealthObservationDto;
import com.manacommunity.api.healthguard.service.HealthDataService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthguard/health-data")
public class HealthDataController {
    private final HealthDataService healthDataService;

    public HealthDataController(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    @GetMapping("/consents")
    public ResponseEntity<List<HealthDataConsentDto>> getConsents(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(healthDataService.getConsents(principal.getId()));
    }

    /** Called only after the resident accepts Mana's storage notice and Android grants the requested scopes. */
    @PutMapping("/consents")
    public ResponseEntity<List<HealthDataConsentDto>> saveConsents(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody HealthDataConsentRequest request) {
        return ResponseEntity.ok(healthDataService.saveConsents(principal.getId(), request));
    }

    @DeleteMapping("/consents/{provider}/{scope}")
    public ResponseEntity<Void> revokeConsent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String provider,
            @PathVariable String scope) {
        healthDataService.revokeConsent(principal.getId(), provider, scope);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/observations")
    public ResponseEntity<Map<String, Integer>> saveObservations(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody HealthObservationBatchRequest request) {
        int accepted = healthDataService.saveObservations(principal.getId(), request);
        return ResponseEntity.ok(Map.of("accepted", accepted));
    }

    @GetMapping("/observations")
    public ResponseEntity<List<HealthObservationDto>> getObservations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {
        return ResponseEntity.ok(healthDataService.getObservations(principal.getId(), from, to));
    }

    @DeleteMapping("/observations")
    public ResponseEntity<Map<String, Long>> deleteObservations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String scope) {
        return ResponseEntity.ok(Map.of("deleted", healthDataService.deleteObservations(principal.getId(), scope)));
    }
}
