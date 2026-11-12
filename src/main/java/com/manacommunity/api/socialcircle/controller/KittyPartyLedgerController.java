package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyLedgerDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyLedgerService;
import com.manacommunity.api.socialcircle.service.KittyPartyReminderScheduler;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kitty-ledger")
@RequiredArgsConstructor
public class KittyPartyLedgerController {

    private final KittyPartyLedgerService ledgerService;
    private final LoggedInUserService loggedInUserService;
    private final KittyPartyReminderScheduler reminderScheduler;

    @PostMapping("/circles/{circleId}/cycles")
    public ResponseEntity<CycleSummaryResponse> createCycle(
            @PathVariable Long circleId,
            @Valid @RequestBody CreateCycleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        CycleSummaryResponse response = ledgerService.createCycle(circleId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/cycles/{cycleId}")
    public ResponseEntity<CycleSummaryResponse> getCycleSummary(@PathVariable Long cycleId) {
        return ResponseEntity.ok(ledgerService.getCycleSummary(cycleId));
    }

    @GetMapping("/circles/{circleId}/active-cycle")
    public ResponseEntity<CycleSummaryResponse> getActiveCycleForCircle(@PathVariable Long circleId) {
        CycleSummaryResponse cycle = ledgerService.getActiveCycleForCircle(circleId);
        if (cycle == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cycle);
    }

    @GetMapping("/cycles/{cycleId}/contributions")
    public ResponseEntity<List<ContributionEntryDto>> getContributions(
            @PathVariable Long cycleId,
            @RequestParam(required = false) Integer periodNumber
    ) {
        return ResponseEntity.ok(ledgerService.getContributionsForPeriod(cycleId, periodNumber));
    }

    @PostMapping("/cycles/{cycleId}/contributions")
    public ResponseEntity<Map<String, Object>> recordContribution(
            @PathVariable Long cycleId,
            @Valid @RequestBody RecordContributionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.recordContribution(cycleId, user, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Contribution recorded successfully"));
    }

    @PostMapping("/contributions/{contributionId}/verify")
    public ResponseEntity<Map<String, Object>> verifyContribution(
            @PathVariable Long contributionId,
            @RequestBody VerifyContributionRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.verifyContribution(contributionId, user, request, idempotencyKey);
        return ResponseEntity.ok(Map.of("success", true, "message", "Contribution verified"));
    }

    @PostMapping("/cycles/{cycleId}/disburse")
    public ResponseEntity<Map<String, Object>> disbursePayout(
            @PathVariable Long cycleId,
            @Valid @RequestBody DisbursePayoutRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.recordPayoutDisbursement(cycleId, user, request, idempotencyKey);
        return ResponseEntity.ok(Map.of("success", true, "message", "Payout disbursement recorded"));
    }

    // =========================================================================
    // ADVANCED WORKFLOW ENDPOINTS
    // =========================================================================

    @PostMapping("/cycles/{cycleId}/swap-rotation")
    public ResponseEntity<CycleSummaryResponse> swapRotation(
            @PathVariable Long cycleId,
            @Valid @RequestBody SwapRotationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        CycleSummaryResponse updatedCycle = ledgerService.adminSwapRotation(cycleId, user, request);
        return ResponseEntity.ok(updatedCycle);
    }

    @PostMapping("/cycles/{cycleId}/remind")
    public ResponseEntity<SendRemindersResponse> sendReminders(
            @PathVariable Long cycleId,
            @RequestBody(required = false) SendRemindersRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        Integer periodNumber = (request != null) ? request.getPeriodNumber() : null;
        SendRemindersResponse response = ledgerService.sendPaymentReminders(cycleId, user, periodNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cycles/{cycleId}/replace-member")
    public ResponseEntity<Map<String, Object>> replaceMember(
            @PathVariable Long cycleId,
            @Valid @RequestBody ReplaceMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.withdrawAndReplaceMember(cycleId, user, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Member successfully replaced in cycle"));
    }

    @GetMapping("/cycles/{cycleId}/statement")
    public ResponseEntity<?> getStatement(
            @PathVariable Long cycleId,
            @RequestParam(required = false) Integer periodNumber,
            @RequestParam(defaultValue = "json") String format
    ) {
        if ("csv".equalsIgnoreCase(format)) {
            String csv = ledgerService.generateStatementCsv(cycleId, periodNumber);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kitty_statement_cycle_" + cycleId + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        }
        return ResponseEntity.ok(ledgerService.generateMonthlyStatement(cycleId, periodNumber));
    }

    @GetMapping("/cycles/{cycleId}/audit-trail")
    public ResponseEntity<List<AuditLogEntryDto>> getAuditTrail(@PathVariable Long cycleId) {
        return ResponseEntity.ok(ledgerService.getAuditTrail(cycleId));
    }

    @PostMapping("/circles/{circleId}/disputes")
    public ResponseEntity<DisputeResponse> createDispute(
            @PathVariable Long circleId,
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        DisputeResponse response = ledgerService.createDispute(circleId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/circles/{circleId}/disputes")
    public ResponseEntity<List<DisputeResponse>> getDisputes(@PathVariable Long circleId) {
        return ResponseEntity.ok(ledgerService.getDisputes(circleId));
    }

    @PostMapping("/circles/{circleId}/disputes/{disputeId}/resolve")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable Long circleId,
            @PathVariable Long disputeId,
            @Valid @RequestBody ResolveDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        DisputeResponse response = ledgerService.resolveDispute(circleId, disputeId, user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/scheduler/trigger-reminders")
    public ResponseEntity<Map<String, Object>> triggerScheduledReminders() {
        int count = reminderScheduler.runAutomatedReminders();
        return ResponseEntity.ok(Map.of("success", true, "dispatchedCount", count));
    }
}
