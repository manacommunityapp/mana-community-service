package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentTimelineEvent;
import com.manacommunity.api.payments.service.PaymentTimelineService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments/timeline")
@RequiredArgsConstructor
public class PaymentTimelineController {
    private final PaymentTimelineService service;

    @GetMapping("/my")
    public List<PaymentTimelineEvent> mine(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestParam(defaultValue = "50") int limit) {
        return service.listForUser(principal.getCommunityId(), principal.getUserId(), limit);
    }

    @GetMapping
    public List<PaymentTimelineEvent> all(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "100") int limit) {
        return service.listForCommunity(principal.getCommunityId(), limit);
    }

    @PostMapping
    public PaymentTimelineEvent record(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestBody PaymentTimelineEvent event) {
        event.setCommunityId(principal.getCommunityId());
        return service.record(event);
    }
}
