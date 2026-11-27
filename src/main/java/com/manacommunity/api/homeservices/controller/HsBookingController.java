package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsBooking;
import com.manacommunity.api.homeservices.service.HsBookingService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home-services/bookings")
@RequiredArgsConstructor
public class HsBookingController {

    private final HsBookingService bookingService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.api.homeservices.service.SlotContentionPricingService slotPricingService;

    @GetMapping("/pending")
    public ResponseEntity<List<HsBooking>> communityPending(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(bookingService.communityPending(p.getCommunityId()));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<HsBooking> assign(@PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(bookingService.assignWorker(id, body.get("workerId")));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<HsBooking> statusPatch(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(bookingService.updateStatus(id, body.get("status")));
    }

    @PutMapping(value = "/{id}/status", params = "type=hs")
    public ResponseEntity<HsBooking> status(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(bookingService.updateStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HsBooking> cancel(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(bookingService.cancel(id, p.getUserId(), p.getCommunityId()));
    }

    @PostMapping("/quote-dynamic")
    public ResponseEntity<com.manacommunity.api.homeservices.service.SlotContentionPricingService.DynamicPriceQuote> quoteDynamicPrice(
            @RequestBody Map<String, Object> body) {
        if (slotPricingService == null) return ResponseEntity.notFound().build();
        java.math.BigDecimal base = new java.math.BigDecimal(body.get("basePrice").toString());
        boolean weekend = Boolean.parseBoolean(body.getOrDefault("isWeekend", false).toString());
        int utilPct = Integer.parseInt(body.getOrDefault("zoneUtilizationPct", 50).toString());
        boolean cluster = Boolean.parseBoolean(body.getOrDefault("sameCommunityCluster", false).toString());
        return ResponseEntity.ok(slotPricingService.calculateDynamicPrice(base, weekend, utilPct, cluster));
    }

    @PostMapping("/slots/{slotId}/hold")
    public ResponseEntity<com.manacommunity.api.homeservices.entity.HsWorkerAvailability> holdSlot(
            @PathVariable Long slotId,
            @RequestBody Map<String, String> body) {
        if (slotPricingService == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(slotPricingService.acquireCheckoutHold(
                slotId, body.get("holdToken"), java.time.Instant.now()));
    }

    @PostMapping("/slots/{slotId}/confirm")
    public ResponseEntity<HsBooking> confirmHeldSlot(
            @PathVariable Long slotId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal p) {
        if (slotPricingService == null) return ResponseEntity.notFound().build();
        String holdToken = body.get("holdToken").toString();
        String flat = body.getOrDefault("flatNumber", "A-101").toString();
        String cat = body.getOrDefault("category", "APPLIANCE").toString();
        java.math.BigDecimal quoted = new java.math.BigDecimal(body.getOrDefault("quotedPrice", "499.00").toString());
        return ResponseEntity.ok(slotPricingService.confirmHeldSlotBooking(
                slotId, holdToken, p.getCommunityId(), p.getUserId(), flat, cat, quoted, java.time.Instant.now()));
    }
}
