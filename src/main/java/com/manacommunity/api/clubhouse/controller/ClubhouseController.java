package com.manacommunity.api.clubhouse.controller;

import com.manacommunity.api.clubhouse.dto.ClubhouseDtos.*;
import com.manacommunity.api.clubhouse.service.ClubhousePosService;
import com.manacommunity.api.clubhouse.service.GuestPassTurnstileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubhouse")
@RequiredArgsConstructor
public class ClubhouseController {

    private final ClubhousePosService posService;
    private final GuestPassTurnstileService passService;

    // ── Clubhouse Café POS & Flat-Tab Settlement ──────────────────────────────

    @GetMapping("/pos/account")
    public ResponseEntity<FlatTabAccountDto> getFlatAccount(
            @RequestParam long societyId,
            @RequestParam long flatId,
            @RequestParam(required = false) String unitCode) {
        return ResponseEntity.ok(posService.getOrProvisionAccount(societyId, flatId, unitCode));
    }

    @PostMapping("/pos/authorize")
    public ResponseEntity<PosAuthorizationResponse> authorizePosOrder(@RequestBody PosOrderRequest request) {
        return ResponseEntity.ok(posService.authorizeOrder(request));
    }

    @GetMapping("/pos/challenges")
    public ResponseEntity<List<PosTabChallengeDto>> listPendingChallenges(
            @RequestParam long societyId,
            @RequestParam long flatId) {
        return ResponseEntity.ok(posService.listPendingChallenges(societyId, flatId));
    }

    @PostMapping("/pos/challenges/{challengeId}/approve")
    public ResponseEntity<PosAuthorizationResponse> resolvePushChallenge(
            @PathVariable UUID challengeId,
            @RequestBody ApproveChallengeRequest request) {
        return ResponseEntity.ok(posService.resolvePushChallenge(challengeId, request));
    }

    @PostMapping("/pos/offline-sync")
    public ResponseEntity<OfflineSyncResultDto> syncOfflineOrders(@RequestBody OfflineSyncBatchRequest request) {
        return ResponseEntity.ok(posService.syncOfflineOrders(request));
    }

    @GetMapping("/pos/snapshot")
    public ResponseEntity<OfflineTerminalSnapshotDto> getOfflineSnapshot(@RequestParam long societyId) {
        return ResponseEntity.ok(posService.getOfflineTerminalSnapshot(societyId));
    }

    @GetMapping("/pos/orders")
    public ResponseEntity<List<PosOrderDto>> listPosOrders(@RequestParam long societyId) {
        return ResponseEntity.ok(posService.listOrders(societyId));
    }

    @PostMapping("/pos/batches/reconcile")
    public ResponseEntity<PosDailyBatchDto> reconcileDailyBatch(@RequestBody ReconcileBatchRequest request) {
        return ResponseEntity.ok(posService.closeAndReconcileDailyBatch(request));
    }

    @GetMapping("/pos/batches")
    public ResponseEntity<List<PosDailyBatchDto>> listDailyBatches(@RequestParam long societyId) {
        return ResponseEntity.ok(posService.listBatches(societyId));
    }

    // ── Pool Guest Passes, Dynamic Pricing & Turnstile QR ────────────────────

    @PostMapping("/passes/quote")
    public ResponseEntity<GuestPassQuoteResponse> quoteGuestPasses(@RequestBody GuestPassQuoteRequest request) {
        return ResponseEntity.ok(passService.quoteGuestPasses(request));
    }

    @PostMapping("/passes/issue")
    public ResponseEntity<IssueGuestPassesResponse> issueGuestPasses(@RequestBody IssueGuestPassesRequest request) {
        return ResponseEntity.ok(passService.issueGuestPasses(request));
    }

    @GetMapping("/passes")
    public ResponseEntity<List<GuestAccessPassDto>> listFlatPasses(
            @RequestParam long societyId,
            @RequestParam long flatId,
            @RequestParam(required = false) String billingMonth) {
        return ResponseEntity.ok(passService.listFlatPasses(societyId, flatId, billingMonth));
    }

    @PostMapping("/passes/{passId}/cancel")
    public ResponseEntity<GuestAccessPassDto> cancelGuestPass(@PathVariable UUID passId) {
        return ResponseEntity.ok(passService.cancelGuestPass(passId));
    }

    @PostMapping("/turnstile/validate")
    public ResponseEntity<TurnstileValidateResponse> validateTurnstileScan(@RequestBody TurnstileValidateRequest request) {
        return ResponseEntity.ok(passService.validateTurnstileScan(request));
    }
}
