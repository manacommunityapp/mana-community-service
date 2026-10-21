package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.service.impl.CposCrmServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

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

    @PutMapping("/leads/{id}/followup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> setFollowUp(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        LocalDateTime followUpAt = body.get("followUpAt") != null
                ? LocalDateTime.parse(body.get("followUpAt"))
                : null;
        return ResponseEntity.ok(crmService.setFollowUp(user, id, followUpAt, body.get("note")));
    }

    @GetMapping("/leads/due-followups")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmLeadDto>> getDueFollowUps(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getDueFollowUps(user));
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

    // ── Portal Inbound Integration ─────────────────────────────────────────

    @GetMapping("/integrations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmInboundConfigDto> getIntegrationConfig(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getOrCreateInboundConfig(user));
    }

    @PutMapping("/integrations/agents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmInboundConfigDto> updateAgentPool(
            @RequestBody Map<String, List<String>> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.updateInboundConfig(user, body.getOrDefault("agents", List.of())));
    }

    // ── RBAC / Team ────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getCrmMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String agentName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        return ResponseEntity.ok(Map.of(
                "role", crmService.getCrmRole(user),
                "agentName", agentName
        ));
    }

    @GetMapping("/agents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getAgents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getAgents(user));
    }

    @PutMapping("/leads/{id}/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> assignLead(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.assignLead(user, id, body.get("agentName")));
    }

    // ── Document Vault ─────────────────────────────────────────────────────

    @PostMapping(value = "/leads/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmDocumentDto> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(crmService.uploadDocument(user, id, file));
    }

    @GetMapping("/leads/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmDocumentDto>> getDocuments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getDocuments(user, id));
    }

    @DeleteMapping("/leads/{id}/documents/{docId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @PathVariable Long docId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        crmService.deleteDocument(user, id, docId);
        return ResponseEntity.noContent().build();
    }

    // ── Call Logging ───────────────────────────────────────────────────────

    @PostMapping("/leads/{id}/calls")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmActivityDto> logCall(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String outcome = (String) body.get("outcome");
        Integer durationMin = body.get("durationMin") != null
                ? ((Number) body.get("durationMin")).intValue() : null;
        String notes = (String) body.get("notes");
        LocalDateTime followUpAt = body.get("followUpAt") != null
                ? LocalDateTime.parse((String) body.get("followUpAt")) : null;
        return ResponseEntity.ok(crmService.logCall(user, id, outcome, durationMin, notes, followUpAt));
    }

    /**
     * Public inbound webhook — authenticated by X-Portal-Key header (API key).
     * Must be added to Spring Security's permitAll list:
     *   .requestMatchers(HttpMethod.POST, "/api/v1/cpos/crm/leads/inbound").permitAll()
     */
    @PostMapping("/leads/inbound")
    public ResponseEntity<CposCrmLeadDto> inboundLead(
            @RequestHeader(value = "X-Portal-Key", required = false) String apiKey,
            @RequestBody CposCrmInboundLeadRequest req) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(crmService.createInboundLead(apiKey, req));
        } catch (com.manacommunity.api.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
