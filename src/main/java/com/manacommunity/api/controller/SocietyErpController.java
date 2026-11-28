package com.manacommunity.api.controller;

import com.manacommunity.api.model.*;
import com.manacommunity.api.service.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/society-erp")
@RequiredArgsConstructor
public class SocietyErpController {

    private final IdempotentBillingService billingService;
    private final LedgerService ledgerService;
    private final TenantIsolationService tenantService;
    private final SocietyRoleSwitchService roleSwitchService;
    private final LoggedInUserService loggedInUserService;

    // ── Idempotent Billing ──────────────────────────────────────────────

    @PostMapping("/billing/run")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> startBillingRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        int month = parseInt(body, "month", java.time.LocalDate.now().getMonthValue());
        int year = parseInt(body, "year", java.time.LocalDate.now().getYear());
        BigDecimal maintenancePerSqft = parseBigDecimal(body, "maintenancePerSqft");
        BigDecimal sinkingFund = parseBigDecimal(body, "sinkingFund");
        BigDecimal waterCharges = parseBigDecimal(body, "waterCharges");
        BigDecimal parkingCharges = parseBigDecimal(body, "parkingCharges");
        boolean applyGst = body.get("applyGst") == null || Boolean.parseBoolean(body.get("applyGst").toString());
        boolean applyPenalty = body.get("applyPenalty") != null && Boolean.parseBoolean(body.get("applyPenalty").toString());
        int dueDays = parseInt(body, "dueDays", 15);

        BillingRun run = billingService.startOrResumeBillingRun(
                communityId, month, year, maintenancePerSqft, sinkingFund,
                waterCharges, parkingCharges, applyGst, applyPenalty, dueDays);

        return ResponseEntity.ok(Map.of(
                "runId", run.getId(),
                "status", run.getStatus(),
                "totalFlats", run.getTotalFlats(),
                "processedFlats", run.getProcessedFlats(),
                "failedFlats", run.getFailedFlats(),
                "totalAmount", run.getTotalAmount(),
                "gstAmount", run.getGstAmount(),
                "penaltyAmount", run.getPenaltyAmount(),
                "idempotencyKey", run.getIdempotencyKey()
        ));
    }

    @GetMapping("/billing/runs")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BillingRun>> getBillingRuns(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(billingService.getBillingRuns(user.getCommunity().getId()));
    }

    @GetMapping("/billing/runs/{runId}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BillingRun> getBillingRunDetail(@PathVariable Long runId) {
        return ResponseEntity.ok(billingService.getBillingRunDetail(runId));
    }

    @GetMapping("/billing/runs/{runId}/items")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BillingRunLineItem>> getBillingRunLineItems(@PathVariable Long runId) {
        return ResponseEntity.ok(billingService.getLineItems(runId));
    }

    // ── Double-Entry Ledger ─────────────────────────────────────────────

    @PostMapping("/ledger/init")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> initializeLedger(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.initializeChartOfAccounts(user.getCommunity().getId());
        return ResponseEntity.ok(Map.of("initialized", true));
    }

    @GetMapping("/ledger/accounts")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<LedgerAccount>> getChartOfAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ledgerService.getChartOfAccounts(user.getCommunity().getId()));
    }

    @PostMapping("/ledger/journal")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> postJournalEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> linesRaw = (List<Map<String, Object>>) body.get("lines");
        List<LedgerService.JournalLine> lines = linesRaw.stream().map(l ->
                new LedgerService.JournalLine(
                        Long.parseLong(l.get("accountId").toString()),
                        l.get("entryType").toString(),
                        new BigDecimal(l.get("amount").toString())
                )).toList();

        String narration = body.get("narration") != null ? body.get("narration").toString() : "";
        String refType = body.get("referenceType") != null ? body.get("referenceType").toString() : null;
        Long refId = body.get("referenceId") != null ? Long.parseLong(body.get("referenceId").toString()) : null;

        String journalId = ledgerService.postJournalEntry(communityId, lines, narration,
                refType, refId, user.getId());

        return ResponseEntity.ok(Map.of("journalId", journalId, "lines", lines.size()));
    }

    @GetMapping("/ledger/journal")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Page<LedgerEntry>> getJournalEntries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ledgerService.getJournalEntries(user.getCommunity().getId(), page, size));
    }

    @GetMapping("/ledger/trial-balance")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> getTrialBalance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int fiscalYear) {
        AppUser user = loggedInUserService.resolve(principal);
        int year = fiscalYear > 0 ? fiscalYear : java.time.LocalDate.now().getYear();
        return ResponseEntity.ok(ledgerService.getTrialBalance(user.getCommunity().getId(), year));
    }

    @PostMapping("/ledger/reverse/{journalId}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> reverseJournal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String journalId) {
        AppUser user = loggedInUserService.resolve(principal);
        String reversalId = ledgerService.reverseJournalEntry(journalId, user.getId());
        return ResponseEntity.ok(Map.of("reversalJournalId", reversalId));
    }

    // ── Multi-Tenant Isolation ──────────────────────────────────────────

    @GetMapping("/tenant/metrics")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> getTenantMetrics(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(tenantService.getTenantMetrics(user.getCommunity().getId()));
    }

    @GetMapping("/tenant/quotas")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<TenantResourceQuota>> getTenantQuotas(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(tenantService.getQuotas(user.getCommunity().getId()));
    }

    @PutMapping("/tenant/quotas/{resourceType}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<TenantResourceQuota> updateQuota(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String resourceType,
            @RequestBody Map<String, Object> body) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();

        return ResponseEntity.ok(tenantService.updateQuota(communityId, resourceType,
                body.get("maxConcurrentQueries") != null ? Integer.parseInt(body.get("maxConcurrentQueries").toString()) : null,
                body.get("maxRowsPerExport") != null ? Integer.parseInt(body.get("maxRowsPerExport").toString()) : null,
                body.get("rateLimitPerMinute") != null ? Integer.parseInt(body.get("rateLimitPerMinute").toString()) : null,
                body.get("priorityWeight") != null ? Integer.parseInt(body.get("priorityWeight").toString()) : null
        ));
    }

    // ── RBAC Role Switching ─────────────────────────────────────────────

    @GetMapping("/roles/my-societies")
    public ResponseEntity<List<Map<String, Object>>> getMySocieties(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(roleSwitchService.getUserSocieties(principal.getId()));
    }

    @PostMapping("/roles/switch")
    public ResponseEntity<Map<String, Object>> switchSociety(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long communityId = Long.parseLong(body.get("communityId").toString());
        String roleType = body.get("roleType").toString();
        return ResponseEntity.ok(roleSwitchService.switchSociety(principal.getId(), communityId, roleType));
    }

    @PostMapping("/roles/assign")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<UserSocietyRole> assignRole(@RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(body.get("userId").toString());
        Long communityId = Long.parseLong(body.get("communityId").toString());
        String roleType = body.get("roleType").toString();
        String flatNo = body.get("flatNo") != null ? body.get("flatNo").toString() : null;
        return ResponseEntity.ok(roleSwitchService.addSocietyRole(userId, communityId, roleType, flatNo));
    }

    @DeleteMapping("/roles/{userId}/{communityId}/{roleType}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> removeRole(
            @PathVariable Long userId, @PathVariable Long communityId, @PathVariable String roleType) {
        roleSwitchService.removeSocietyRole(userId, communityId, roleType);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    @GetMapping("/roles/community-members")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getCommunityMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(roleSwitchService.getCommunityMembers(user.getCommunity().getId()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static int parseInt(Map<String, Object> body, String key, int defaultValue) {
        return body.get(key) != null ? Integer.parseInt(body.get(key).toString()) : defaultValue;
    }

    private static BigDecimal parseBigDecimal(Map<String, Object> body, String key) {
        return body.get(key) != null ? new BigDecimal(body.get(key).toString()) : BigDecimal.ZERO;
    }
}
