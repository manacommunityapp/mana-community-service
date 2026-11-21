package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.CourtSlotSubscription;
import com.manacommunity.api.sportsops.service.CourtSubscriptionService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sports-ops/subscriptions")
@RequiredArgsConstructor
public class CourtSubscriptionController {
    private final CourtSubscriptionService service;

    @GetMapping
    public List<CourtSlotSubscription> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.list(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<CourtSlotSubscription> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public CourtSlotSubscription create(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody CourtSlotSubscription sub) {
        sub.setCommunityId(principal.getCommunityId());
        sub.setUserId(principal.getUserId());
        return service.create(sub);
    }

    @PutMapping("/{id}/cancel")
    public CourtSlotSubscription cancel(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id) {
        return service.cancel(principal.getCommunityId(), id);
    }
}
