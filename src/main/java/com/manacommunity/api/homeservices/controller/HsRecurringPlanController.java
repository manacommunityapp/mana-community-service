package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsRecurringPlan;
import com.manacommunity.api.homeservices.service.HsRecurringPlanService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-services/plans")
@RequiredArgsConstructor
public class HsRecurringPlanController {

    private final HsRecurringPlanService planService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.api.homeservices.service.MorningSubscriptionEngineService morningEngine;

    @PostMapping
    public ResponseEntity<HsRecurringPlan> create(@RequestBody HsRecurringPlan plan,
            @AuthenticationPrincipal UserPrincipal p) {
        plan.setCommunityId(p.getCommunityId());
        plan.setUserId(p.getUserId());
        return ResponseEntity.ok(planService.create(plan));
    }

    @GetMapping("/my")
    public ResponseEntity<List<HsRecurringPlan>> myPlans(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(planService.myPlans(p.getUserId()));
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<HsRecurringPlan> pause(@PathVariable Long id) {
        return ResponseEntity.ok(planService.pause(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HsRecurringPlan> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(planService.cancel(id));
    }

    @PutMapping("/{id}/override")
    public ResponseEntity<com.manacommunity.api.homeservices.service.MorningSubscriptionEngineService.OverrideResult> upsertDailyOverride(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
        if (morningEngine == null) return ResponseEntity.notFound().build();
        java.time.LocalDate date = java.time.LocalDate.parse(body.get("deliveryDate").toString());
        Integer qty = body.containsKey("overrideQty") ? Integer.valueOf(body.get("overrideQty").toString()) : 1;
        boolean paused = body.containsKey("paused") && Boolean.parseBoolean(body.get("paused").toString());
        return ResponseEntity.ok(morningEngine.upsertOverride(id, date, qty, paused, java.time.LocalDateTime.now()));
    }

    @PostMapping("/manifest/{vendorId}")
    public ResponseEntity<com.manacommunity.api.homeservices.service.MorningSubscriptionEngineService.VendorRouteManifest> freezeAndBuildManifest(
            @PathVariable Long vendorId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (morningEngine == null) return ResponseEntity.notFound().build();
        java.time.LocalDate date = (body != null && body.containsKey("deliveryDate"))
                ? java.time.LocalDate.parse(body.get("deliveryDate"))
                : java.time.LocalDate.now().plusDays(1);
        return ResponseEntity.ok(morningEngine.freezeAndBuildVendorManifest(vendorId, date, (uid, amt) -> true));
    }

    @PostMapping("/deliveries/{instanceId}/settle")
    public ResponseEntity<com.manacommunity.api.homeservices.entity.MorningDeliveryInstance> settleDeliveryOutcome(
            @PathVariable Long instanceId,
            @RequestBody java.util.Map<String, Object> body) {
        if (morningEngine == null) return ResponseEntity.notFound().build();
        boolean delivered = Boolean.parseBoolean(body.getOrDefault("delivered", true).toString());
        String reason = body.containsKey("failureReason") && body.get("failureReason") != null
                ? body.get("failureReason").toString()
                : null;
        return ResponseEntity.ok(morningEngine.settleDeliveryOutcome(instanceId, delivered, reason));
    }

    @PostMapping("/deliveries/sweep-refund")
    public ResponseEntity<java.util.Map<String, Object>> sweepUnmarkedRefunds(
            @RequestBody(required = false) java.util.Map<String, String> body) {
        if (morningEngine == null) return ResponseEntity.notFound().build();
        java.time.LocalDate date = (body != null && body.containsKey("deliveryDate"))
                ? java.time.LocalDate.parse(body.get("deliveryDate"))
                : java.time.LocalDate.now();
        int count = morningEngine.sweepUnmarkedToAutoRefund(date);
        return ResponseEntity.ok(java.util.Map.of("deliveryDate", date.toString(), "autoRefundedCount", count));
    }
}
