package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.VendorPrepAnalyticsService;
import com.manacommunity.api.food.service.VendorPrepAnalyticsService.VendorPrepSummary;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/analytics/prep")
@RequiredArgsConstructor
public class VendorPrepAnalyticsController {

    private final VendorPrepAnalyticsService analyticsService;

    @PostMapping("/{orderId}/start")
    public ResponseEntity<Void> recordStart(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        analyticsService.recordPrepStart(
            Long.parseLong(body.get("vendorId").toString()),
            principal.getCommunityId(), orderId,
            body.get("orderType").toString(),
            body.containsKey("orderPlacedAt") ? Instant.parse(body.get("orderPlacedAt").toString()) : Instant.now()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<Void> recordComplete(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {
        analyticsService.recordPrepComplete(
            Long.parseLong(body.get("vendorId").toString()),
            orderId, body.get("orderType").toString(),
            body.containsKey("slotId") ? Long.parseLong(body.get("slotId").toString()) : null
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<VendorPrepSummary> getAnalytics(
            @RequestParam Long vendorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getAnalytics(vendorId, from, to));
    }
}
