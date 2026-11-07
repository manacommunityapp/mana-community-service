package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyLedgerDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyLedgerService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.verifyContribution(contributionId, user, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Contribution verified"));
    }

    @PostMapping("/cycles/{cycleId}/disburse")
    public ResponseEntity<Map<String, Object>> disbursePayout(
            @PathVariable Long cycleId,
            @Valid @RequestBody DisbursePayoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        ledgerService.recordPayoutDisbursement(cycleId, user, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Payout disbursement recorded"));
    }
}
