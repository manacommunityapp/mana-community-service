package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.service.impl.CposCrmServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.manacommunity.api.user.security.UserPrincipal;

@RestController
@RequestMapping("/api/v1/cpos/crm")
@RequiredArgsConstructor
public class CposCrmController {

    private final CposCrmServiceImpl crmService;
    private final LoggedInUserService loggedInUserService;

    // ── Leads ──────────────────────────────────────────────────────────────

    @GetMapping("/leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmLeadDto>> getLeads(
            @RequestParam(required = false) String stage,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getLeads(user, stage));
    }

    @GetMapping("/leads/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> getLead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getLeadById(user, id));
    }

    @PostMapping("/leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> createLead(
            @RequestBody CposCrmLeadRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.createLead(user, req));
    }

    @PutMapping("/leads/{id}/stage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> updateStage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.updateStage(user, id, body.get("stage")));
    }

    @PostMapping("/leads/{id}/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmActivityDto> addActivity(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(
                crmService.addActivity(user, id, body.get("type"), body.get("note")));
    }

    @DeleteMapping("/leads/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteLead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        crmService.deleteLead(user, id);
        return ResponseEntity.noContent().build();
    }

    // ── Analytics ──────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmAnalyticsDto> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getAnalytics(user));
    }

    // ── Brokers ────────────────────────────────────────────────────────────

    @GetMapping("/brokers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposBrokerDto>> getBrokers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getBrokers(user));
    }

    @PostMapping("/brokers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposBrokerDto> createBroker(
            @RequestBody CposBrokerDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.createBroker(user, req));
    }

    @GetMapping("/commissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposBrokerCommissionDto>> getCommissions(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getCommissions(user));
    }

    @PostMapping("/brokers/{brokerId}/link/{leadId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposBrokerCommissionDto> linkBroker(
            @PathVariable Long brokerId,
            @PathVariable Long leadId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.linkBrokerToLead(user, brokerId, leadId));
    }

    @PutMapping("/commissions/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposBrokerCommissionDto> approveCommission(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.approveCommission(user, id));
    }
}
