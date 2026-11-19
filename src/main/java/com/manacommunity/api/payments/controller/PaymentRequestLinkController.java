package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentRequestLink;
import com.manacommunity.api.payments.service.PaymentRequestLinkService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments/request-links")
@RequiredArgsConstructor
public class PaymentRequestLinkController {
    private final PaymentRequestLinkService service;

    @GetMapping("/my")
    public List<PaymentRequestLink> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listCreatedBy(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public PaymentRequestLink create(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody PaymentRequestLink link) {
        link.setCommunityId(principal.getCommunityId());
        link.setCreatedBy(principal.getUserId());
        return service.create(link);
    }

    @PutMapping("/{id}/paid")
    public PaymentRequestLink markPaid(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        return service.markPaid(principal.getCommunityId(), id, principal.getUserId());
    }
}
