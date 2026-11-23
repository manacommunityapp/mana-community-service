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
}
