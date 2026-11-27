package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrTimeGuardAnomaly;
import com.manacommunity.api.hrm.repository.HrTimeGuardAnomalyRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/timeguard")
@RequiredArgsConstructor
public class HrTimeGuardController {

    private final HrTimeGuardAnomalyRepository anomalyRepository;

    @GetMapping("/{orgId}/anomalies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrTimeGuardAnomaly>> getAnomalies(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<HrTimeGuardAnomaly> list = anomalyRepository.findByOrganizationIdOrderByEventTimestampDesc(orgId);
        if (list.isEmpty()) {
            // Seed sample anomalies for initial dashboard view
            Long commId = principal != null && principal.getCommunityId() != null ? principal.getCommunityId() : 1L;
            HrTimeGuardAnomaly anom1 = anomalyRepository.save(HrTimeGuardAnomaly.builder()
                    .id("anom_" + System.currentTimeMillis() + "_1")
                    .organizationId(orgId)
                    .communityId(commId)
                    .anomalyType("GEO_JUMP")
                    .severity("CRITICAL")
                    .personName("Ramesh Pawar")
                    .personRole("Contractor Housekeeper")
                    .agencyName("Apex Facility Solutions Pvt Ltd")
                    .gateLocation("South Gate (TS-GATE-SOUTH-02)")
                    .confidenceScore(BigDecimal.valueOf(0.94))
                    .forensicDetails("Punched at North Gate at 08:02 AM, then South Gate at 08:05 AM (1.2 km within 180s - impossible travel speed).")
                    .status("FLAGGED")
                    .eventTimestamp(LocalDateTime.now().minusHours(2))
                    .build());

            HrTimeGuardAnomaly anom2 = anomalyRepository.save(HrTimeGuardAnomaly.builder()
                    .id("anom_" + System.currentTimeMillis() + "_2")
                    .organizationId(orgId)
                    .communityId(commId)
                    .anomalyType("BUDDY_PUNCH_SUSPECTED")
                    .severity("CRITICAL")
                    .personName("Unknown Proxy")
                    .personRole("Contract Security")
                    .agencyName("G4S Global Security Services")
                    .gateLocation("Main Gate Turnstile 01")
                    .confidenceScore(BigDecimal.valueOf(0.68))
                    .forensicDetails("Low facial similarity match attempting to punch on behalf of Guard Vikram Rathore (Confidence 68% vs 85% required threshold).")
                    .status("FLAGGED")
                    .eventTimestamp(LocalDateTime.now().minusMinutes(45))
                    .build());

            list = List.of(anom1, anom2);
        }
        return ResponseEntity.ok(list);
    }

    @Data
    public static class ResolveAnomalyRequest {
        private String status;
        private String actionTaken;
    }

    @PostMapping("/{orgId}/anomalies/{anomalyId}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTimeGuardAnomaly> resolveAnomaly(
            @PathVariable Long orgId,
            @PathVariable String anomalyId,
            @RequestBody ResolveAnomalyRequest req) {
        HrTimeGuardAnomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));
        if (req.getStatus() != null) anomaly.setStatus(req.getStatus());
        if (req.getActionTaken() != null) anomaly.setActionTaken(req.getActionTaken());
        return ResponseEntity.ok(anomalyRepository.save(anomaly));
    }
}
