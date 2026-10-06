package com.manacommunity.api.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/api/cpos", "/api/api/v1/cpos"})
@RequiredArgsConstructor
public class CposAliasController {

    private final LoggedInUserService loggedInUserService;

    @GetMapping("/properties/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyProperties(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("id", user.getId());
        property.put("unitNumber", user.getFlatNo());
        property.put("ownerName", user.getFullName());
        property.put("communityId", user.getCommunity().getId());
        property.put("communityName", user.getCommunity().getName());
        property.put("type", "RESIDENTIAL");
        property.put("status", "OCCUPIED");
        return ResponseEntity.ok(List.of(property));
    }

    @GetMapping("/nocs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getNocs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of());
        result.put("totalElements", 0);
        result.put("totalPages", 0);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/nocs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> requestNoc(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Map<String, Object> noc = new LinkedHashMap<>();
        noc.put("id", System.currentTimeMillis());
        noc.put("applicantName", user.getFullName());
        noc.put("unitNumber", user.getFlatNo());
        noc.put("type", request.getOrDefault("type", "GENERAL"));
        noc.put("status", "PENDING");
        noc.put("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(noc);
    }

    @GetMapping("/finance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFinanceSummary(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> finance = new LinkedHashMap<>();
        finance.put("totalRevenue", BigDecimal.ZERO);
        finance.put("pendingDues", BigDecimal.ZERO);
        finance.put("collected", BigDecimal.ZERO);
        finance.put("defaulters", 0);
        finance.put("period", LocalDate.now().getMonth().toString() + " " + LocalDate.now().getYear());
        return ResponseEntity.ok(finance);
    }

    @GetMapping("/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of());
        result.put("totalElements", 0);
        result.put("totalPages", 0);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ai-insights")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAiInsights(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("communityId", communityId);
        insights.put("insights", List.of());
        insights.put("generatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/units")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUnits(
            @RequestParam(required = false) String tower,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of());
        result.put("totalElements", 0);
        result.put("totalPages", 0);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/towers/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTowersSummary(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("towers", List.of());
        summary.put("totalTowers", 0);
        summary.put("totalUnits", 0);
        summary.put("occupiedUnits", 0);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/tenants/kyc")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTenantsKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of());
        result.put("totalElements", 0);
        result.put("totalPages", 0);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tenants/kyc/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTenantsKycSummary(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTenants", 0);
        summary.put("kycCompleted", 0);
        summary.put("kycPending", 0);
        summary.put("kycRejected", 0);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/crm/leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCrmLeads(
            @RequestParam(required = false) String stage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of());
        result.put("totalElements", 0);
        result.put("totalPages", 0);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/crm/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCrmSummary(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalLeads", 0);
        summary.put("newLeads", 0);
        summary.put("qualified", 0);
        summary.put("converted", 0);
        summary.put("lost", 0);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/crm/leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createLead(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Map<String, Object> lead = new LinkedHashMap<>();
        lead.put("id", System.currentTimeMillis());
        lead.put("name", request.getOrDefault("name", ""));
        lead.put("email", request.getOrDefault("email", ""));
        lead.put("phone", request.getOrDefault("phone", ""));
        lead.put("stage", "NEW");
        lead.put("createdBy", user.getFullName());
        lead.put("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(lead);
    }

    @PutMapping("/crm/leads/{id}/stage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateLeadStage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("stage", request.getOrDefault("stage", "QUALIFIED"));
        result.put("updatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }
}
