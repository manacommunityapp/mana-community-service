package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.CourtSlotWaitlist;
import com.manacommunity.api.sportsops.service.WaitlistService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sports-ops/waitlist")
@RequiredArgsConstructor
public class WaitlistController {
    private final WaitlistService service;

    @GetMapping("/booking/{bookingId}")
    public List<CourtSlotWaitlist> byBooking(@PathVariable Long bookingId) {
        return service.listByBooking(bookingId);
    }

    @GetMapping("/my")
    public List<CourtSlotWaitlist> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public CourtSlotWaitlist add(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestBody CourtSlotWaitlist entry) {
        entry.setCommunityId(principal.getCommunityId());
        entry.setUserId(principal.getUserId());
        return service.add(entry);
    }

    @PutMapping("/{id}/notify")
    public CourtSlotWaitlist markNotified(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id) {
        return service.markNotified(principal.getCommunityId(), id);
    }
}
