package com.manacommunity.api.accounting.controller;

import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.service.ImmutableAuditChainService;
import com.manacommunity.api.accounting.service.StatutoryTaxRuleService;
import com.manacommunity.api.accounting.service.TallySyncService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class StatutoryAccountingController {

    private final StatutoryTaxRuleService taxRuleService;
    private final ImmutableAuditChainService auditChainService;
    private final TallySyncService tallySyncService;

    // ── Versioned GST & TDS Rule Engine ───────────────────────────────────────

    @GetMapping("/tax-rules")
    public ResponseEntity<List<TaxRuleVersionDto>> listTaxRules(
            @RequestParam(required = false) String ruleFamily,
            @RequestParam(required = false) String fyCode) {
        return ResponseEntity.ok(taxRuleService.listRuleVersions(ruleFamily, fyCode));
    }

    @PostMapping("/tax-rules")
    public ResponseEntity<TaxRuleVersionDto> publishTaxRule(@RequestBody PublishTaxRuleRequest request) {
        return ResponseEntity.ok(taxRuleService.publishRuleVersion(request));
    }

    @PostMapping("/gst/calculate")
    public ResponseEntity<MaintenanceGstResult> calculateMaintenanceGst(@RequestBody MaintenanceGstRequest request) {
        return ResponseEntity.ok(taxRuleService.calculateMaintenanceGst(request));
    }

    @PostMapping("/tds/calculate")
    public ResponseEntity<VendorTdsResult> calculateVendorTds(@RequestBody VendorTdsRequest request) {
        return ResponseEntity.ok(taxRuleService.calculateVendorTds(request));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<RecalculationBatchDto> recalculateMidFy(@RequestBody RecalculateTaxRequest request) {
        return ResponseEntity.ok(taxRuleService.recalculateRange(request));
    }

    // ── Hash-Chained Immutable Audit Ledger ───────────────────────────────────

    @GetMapping("/audit-chain/entries")
    public ResponseEntity<List<AuditLedgerEntryDto>> listAuditEntries(@RequestParam long societyId) {
        return ResponseEntity.ok(auditChainService.listEntries(societyId));
    }

    @PostMapping("/audit-chain/post")
    public ResponseEntity<TallyOutboxItemDto> postToAuditChainAndTallyOutbox(
            @RequestBody PostAuditEntryRequest request,
            @RequestParam(required = false) String companyName) {
        return ResponseEntity.ok(tallySyncService.postAndEnqueueCloudVoucher(request, companyName));
    }

    @GetMapping("/audit-chain/verify")
    public ResponseEntity<AuditChainVerificationDto> verifyAuditChain(@RequestParam long societyId) {
        return ResponseEntity.ok(auditChainService.verifyChain(societyId));
    }

    // ── Conflict-Safe Bidirectional Tally Sync ────────────────────────────────

    @GetMapping("/tally-sync/summary")
    public ResponseEntity<TallySyncSummaryDto> getTallySummary(@RequestParam long societyId) {
        return ResponseEntity.ok(tallySyncService.getSummary(societyId));
    }

    @GetMapping("/tally-sync/outbox")
    public ResponseEntity<List<TallyOutboxItemDto>> getTallyOutbox(@RequestParam long societyId) {
        return ResponseEntity.ok(tallySyncService.getPendingOutbox(societyId));
    }

    @PostMapping("/tally-sync/ack")
    public ResponseEntity<Void> acknowledgeTallyPush(@RequestBody TallyAckRequest request) {
        tallySyncService.acknowledgeOutbox(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tally-sync/inbound")
    public ResponseEntity<Map<String, Object>> ingestInboundFromTally(@RequestBody TallyInboundSyncRequest request) {
        return ResponseEntity.ok(tallySyncService.ingestInboundFromTally(request));
    }

    @GetMapping("/tally-sync/conflicts")
    public ResponseEntity<List<TallySyncConflictDto>> listConflicts(@RequestParam long societyId) {
        return ResponseEntity.ok(tallySyncService.listOpenConflicts(societyId));
    }

    @PostMapping("/tally-sync/conflicts/{conflictId}/resolve")
    public ResponseEntity<Void> resolveConflict(
            @PathVariable String conflictId,
            @RequestBody ResolveConflictRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        tallySyncService.resolveConflict(conflictId, request, userId);
        return ResponseEntity.ok().build();
    }
}
