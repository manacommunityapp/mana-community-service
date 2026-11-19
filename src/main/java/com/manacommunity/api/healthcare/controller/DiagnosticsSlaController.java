package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.DiagnosticsSla;
import com.manacommunity.api.healthcare.service.DiagnosticsSlaService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthcare/diagnostics-sla")
@RequiredArgsConstructor
public class DiagnosticsSlaController {

    private final DiagnosticsSlaService slaService;

    @PostMapping
    public ResponseEntity<DiagnosticsSla> create(
            @RequestBody DiagnosticsSla sla,
            @AuthenticationPrincipal UserPrincipal principal) {
        sla.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(slaService.create(sla));
    }

    @PutMapping("/{id}/delivered")
    public ResponseEntity<DiagnosticsSla> markDelivered(@PathVariable Long id) {
        return ResponseEntity.ok(slaService.markReportDelivered(id));
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<DiagnosticsSla> escalate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(slaService.escalate(id, body.get("notes")));
    }

    @GetMapping
    public ResponseEntity<List<DiagnosticsSla>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(slaService.getByCommunity(principal.getCommunityId()));
    }

    @GetMapping("/breaches")
    public ResponseEntity<List<DiagnosticsSla>> breaches(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(slaService.getBreaches(principal.getCommunityId()));
    }
}
