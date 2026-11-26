package com.manacommunity.api.controller;

import com.manacommunity.api.billing.engine.FeeContext;
import com.manacommunity.api.billing.engine.FeeEngine;
import com.manacommunity.api.notification.router.NotificationRequest;
import com.manacommunity.api.notification.router.NotificationRouter;
import com.manacommunity.api.ratelimit.GateApiRateLimitService;
import com.manacommunity.api.ratelimit.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/lld")
@RequiredArgsConstructor
public class LldShowcaseController {

    private final FeeEngine feeEngine;
    private final GateApiRateLimitService rateLimitService;
    private final NotificationRouter notificationRouter;

    // ── Fee Engine ──

    @PostMapping("/fee-engine/calculate")
    public ResponseEntity<FeeEngine.InvoiceBreakdown> calculateFees(@RequestBody FeeCalcRequest req) {
        FeeContext ctx = new FeeContext(
                req.communityId(), req.residentId(), req.flatNo(),
                req.flatAreaSqft() != null ? new BigDecimal(req.flatAreaSqft()) : BigDecimal.ZERO,
                req.flatType(), LocalDate.now(), req.overdueMonths(),
                req.outstandingAmount() != null ? new BigDecimal(req.outstandingAmount()) : BigDecimal.ZERO,
                req.gstExempt(), req.hasActiveWaiver());
        return ResponseEntity.ok(feeEngine.calculate(ctx));
    }

    @GetMapping("/fee-engine/rules")
    public ResponseEntity<?> listFeeRules() {
        return ResponseEntity.ok(feeEngine.listRules());
    }

    // ── Rate Limiter ──

    @PostMapping("/rate-limiter/check")
    public ResponseEntity<GateApiRateLimitService.RateLimitDecision> checkRate(
            @RequestParam String partnerId, @RequestParam Long societyId) {
        return ResponseEntity.ok(rateLimitService.checkPartnerRate(partnerId, societyId));
    }

    @GetMapping("/rate-limiter/partner-stats")
    public ResponseEntity<Map<String, SlidingWindowRateLimiter.RateLimitInfo>> partnerStats() {
        return ResponseEntity.ok(rateLimitService.getPartnerStats());
    }

    @GetMapping("/rate-limiter/society-stats/{societyId}")
    public ResponseEntity<SlidingWindowRateLimiter.RateLimitInfo> societyStats(@PathVariable Long societyId) {
        return ResponseEntity.ok(rateLimitService.getSocietyStats(societyId));
    }

    @PostMapping("/rate-limiter/set-limit")
    public ResponseEntity<Map<String, String>> setPartnerLimit(
            @RequestParam String partnerId, @RequestParam int limit) {
        rateLimitService.setPartnerBaseLimit(partnerId, limit);
        return ResponseEntity.ok(Map.of("status", "updated", "partnerId", partnerId, "newLimit", String.valueOf(limit)));
    }

    // ── Notification Router ──

    @PostMapping("/notification-router/send")
    public ResponseEntity<NotificationRouter.RoutingResult> sendNotification(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationRouter.route(request));
    }

    @GetMapping("/notification-router/channels")
    public ResponseEntity<?> listChannels() {
        return ResponseEntity.ok(notificationRouter.listChannels());
    }

    @PostMapping("/notification-router/dnd")
    public ResponseEntity<Map<String, String>> setDnd(@RequestParam Long userId, @RequestParam boolean enabled) {
        notificationRouter.setDnd(userId, enabled);
        return ResponseEntity.ok(Map.of("userId", userId.toString(), "dnd", String.valueOf(enabled)));
    }

    // ── DTOs ──

    public record FeeCalcRequest(Long communityId, Long residentId, String flatNo, String flatAreaSqft,
                                  String flatType, int overdueMonths, String outstandingAmount,
                                  boolean gstExempt, boolean hasActiveWaiver) {}
}
