package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsWorkerPayout;
import com.manacommunity.api.homeservices.service.HsPayoutService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-services/payouts")
@RequiredArgsConstructor
public class HsPayoutController {

    private final HsPayoutService payoutService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.api.homeservices.service.SplitSettlementLedgerService settlementService;

    @PostMapping
    public ResponseEntity<HsWorkerPayout> create(@RequestBody HsWorkerPayout payout,
            @AuthenticationPrincipal UserPrincipal p) {
        payout.setCommunityId(p.getCommunityId());
        return ResponseEntity.ok(payoutService.create(payout));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<HsWorkerPayout>> statement(@PathVariable Long workerId) {
        return ResponseEntity.ok(payoutService.workerStatement(workerId));
    }

    @GetMapping("/unpaid")
    public ResponseEntity<List<HsWorkerPayout>> unpaid(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(payoutService.unpaidPayouts(p.getCommunityId()));
    }

    @PutMapping("/{id}/paid")
    public ResponseEntity<HsWorkerPayout> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(payoutService.markPaid(id));
    }

    @PostMapping("/multi-tender/initiate")
    public ResponseEntity<com.manacommunity.api.homeservices.service.SplitSettlementLedgerService.SettlementWithLedger> initiateSplitOrder(
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal p) {
        if (settlementService == null) return ResponseEntity.notFound().build();
        var req = com.manacommunity.api.homeservices.service.SplitSettlementLedgerService.MultiTenderQuoteRequest.builder()
                .orderRef(body.get("orderRef").toString())
                .bookingId(body.containsKey("bookingId") && body.get("bookingId") != null ? Long.valueOf(body.get("bookingId").toString()) : null)
                .communityId(p.getCommunityId())
                .vendorId(Long.valueOf(body.get("vendorId").toString()))
                .userId(p.getUserId())
                .grossAmount(new java.math.BigDecimal(body.get("grossAmount").toString()))
                .pointsUsed(new java.math.BigDecimal(body.getOrDefault("pointsUsed", "0").toString()))
                .couponCode(body.get("couponCode") != null ? body.get("couponCode").toString() : null)
                .couponDiscount(new java.math.BigDecimal(body.getOrDefault("couponDiscount", "0").toString()))
                .upiAmount(new java.math.BigDecimal(body.getOrDefault("upiAmount", "0").toString()))
                .build();
        return ResponseEntity.ok(settlementService.initiateAndReserve(req));
    }

    @PostMapping("/multi-tender/{orderRef}/complete")
    public ResponseEntity<com.manacommunity.api.homeservices.entity.MultiTenderSettlement> completeSplitOrder(
            @PathVariable String orderRef,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        if (settlementService == null) return ResponseEntity.notFound().build();
        boolean done = body == null || !body.containsKey("jobCompleted") || Boolean.parseBoolean(body.get("jobCompleted").toString());
        return ResponseEntity.ok(settlementService.markCapturedAndEligibleOnCompletion(orderRef, done));
    }

    @PostMapping("/multi-tender/{orderRef}/compensate")
    public ResponseEntity<com.manacommunity.api.homeservices.service.SplitSettlementLedgerService.SettlementWithLedger> compensateSplitOrder(
            @PathVariable String orderRef,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (settlementService == null) return ResponseEntity.notFound().build();
        String reason = body != null ? body.getOrDefault("reason", "UPI_TIMEOUT") : "UPI_TIMEOUT";
        return ResponseEntity.ok(settlementService.compensateOnUpiFailure(orderRef, reason));
    }
}
