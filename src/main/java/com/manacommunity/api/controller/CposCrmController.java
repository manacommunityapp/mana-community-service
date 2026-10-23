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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
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

    // ── Export ────────────────────────────────────────────────────────────

    @GetMapping(value = "/export/leads.csv", produces = "text/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportLeadsCsv(
            @RequestParam(required = false) String stage,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String csv = crmService.exportLeadsCsv(user, stage);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"crm-leads.csv\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    // ── Email Templates ────────────────────────────────────────────────────

    @GetMapping("/email-templates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmEmailTemplateDto>> getEmailTemplates(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getEmailTemplates(user));
    }

    @PostMapping("/email-templates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmEmailTemplateDto> createEmailTemplate(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.createEmailTemplate(user, body));
    }

    @PutMapping("/email-templates/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmEmailTemplateDto> updateEmailTemplate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.updateEmailTemplate(user, id, body));
    }

    @DeleteMapping("/email-templates/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEmailTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        crmService.deleteEmailTemplate(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leads/{id}/send-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmActivityDto> sendLeadEmail(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.sendLeadEmail(user, id, body));
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

    // ── Duplicate check ────────────────────────────────────────────────────

    @GetMapping("/leads/check-duplicate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean dup = crmService.isDuplicateLead(user.getCommunity().getId(), phone, email);
        return ResponseEntity.ok(Map.of("duplicate", dup));
    }

    // ── CSV Import ─────────────────────────────────────────────────────────

    @PostMapping(value = "/leads/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CrmImportResultDto> importLeads(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        AppUser user = loggedInUserService.resolve(principal);
        String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(crmService.importLeadsCsv(user, csv));
    }

    // ── Tags ───────────────────────────────────────────────────────────────

    @GetMapping("/tags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmTagDto>> getTags(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getTags(user));
    }

    @PostMapping("/tags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmTagDto> createTag(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(crmService.createTag(user, body.get("name"), body.get("color")));
    }

    @DeleteMapping("/tags/{tagId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        crmService.deleteTag(user, tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leads/{id}/tags/{tagId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> addTagToLead(
            @PathVariable Long id, @PathVariable Long tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.addTagToLead(user, id, tagId));
    }

    @DeleteMapping("/leads/{id}/tags/{tagId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> removeTagFromLead(
            @PathVariable Long id, @PathVariable Long tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.removeTagFromLead(user, id, tagId));
    }

    // ── Sales Targets ──────────────────────────────────────────────────────

    @GetMapping("/targets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmSalesTargetDto>> getSalesTargets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        YearMonth now = YearMonth.now();
        return ResponseEntity.ok(crmService.getSalesTargets(user,
                month != null ? month : now.getMonthValue(),
                year  != null ? year  : now.getYear()));
    }

    @PutMapping("/targets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmSalesTargetDto> upsertSalesTarget(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        YearMonth now = YearMonth.now();
        return ResponseEntity.ok(crmService.upsertSalesTarget(
                user,
                (String) body.get("agentName"),
                body.get("month") != null ? ((Number) body.get("month")).intValue() : now.getMonthValue(),
                body.get("year")  != null ? ((Number) body.get("year")).intValue()  : now.getYear(),
                body.get("targetLeadsClosed")  != null ? ((Number) body.get("targetLeadsClosed")).intValue()  : 0,
                body.get("targetRevenueLakhs") != null ? ((Number) body.get("targetRevenueLakhs")).intValue() : 0));
    }

    // ── Leaderboard ────────────────────────────────────────────────────────

    @GetMapping("/leaderboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmAnalyticsDto.LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        YearMonth now = YearMonth.now();
        return ResponseEntity.ok(crmService.getLeaderboard(user,
                month != null ? month : now.getMonthValue(),
                year  != null ? year  : now.getYear()));
    }

    // ── Stale Leads ────────────────────────────────────────────────────────

    @GetMapping("/leads/stale")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmLeadDto>> getStaleLeads(
            @RequestParam(defaultValue = "7") int staleDays,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getStaleLeads(user, staleDays));
    }

    // ── Bulk Email Campaigns ───────────────────────────────────────────────

    @GetMapping("/email-campaigns")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CrmEmailCampaignDto>> getEmailCampaigns(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getEmailCampaigns(user));
    }

    @PostMapping("/email-campaigns")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CrmEmailCampaignDto> sendBulkEmailCampaign(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.sendBulkEmailCampaign(user, body));
    }

    // ── SMS ────────────────────────────────────────────────────────────────

    @PostMapping("/leads/{id}/sms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> sendLeadSms(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        crmService.sendLeadSms(user, id, body.getOrDefault("message", ""));
        return ResponseEntity.ok().build();
    }

    // ── Approval Workflows ─────────────────────────────────────────────────

    @GetMapping("/leads/pending-approval")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmLeadDto>> getLeadsAwaitingApproval(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getLeadsAwaitingApproval(user));
    }

    @PostMapping("/leads/{id}/request-approval")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> requestApproval(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.requestApproval(user, id, body.getOrDefault("reason", "")));
    }

    @PostMapping("/leads/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmLeadDto> approveOrRejectLead(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String comment = body.containsKey("comment") ? body.get("comment").toString() : null;
        return ResponseEntity.ok(crmService.approveOrRejectLead(user, id, approve, comment));
    }

    // ── Email Drip Sequences ───────────────────────────────────────────────

    @GetMapping("/drip-sequences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CposCrmDripSequenceDto>> getDripSequences(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.getDripSequences(user));
    }

    @PostMapping("/drip-sequences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CposCrmDripSequenceDto> createDripSequence(
            @RequestBody CposCrmDripSequenceDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(crmService.createDripSequence(user, req));
    }

    @DeleteMapping("/drip-sequences/{seqId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDripSequence(
            @PathVariable Long seqId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        crmService.deleteDripSequence(user, seqId);
        return ResponseEntity.noContent().build();
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
