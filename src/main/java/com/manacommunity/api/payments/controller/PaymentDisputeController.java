package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentDispute;
import com.manacommunity.api.payments.service.PaymentDisputeService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/disputes")
@RequiredArgsConstructor
public class PaymentDisputeController {
    private final PaymentDisputeService service;

    @GetMapping
    public List<PaymentDispute> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listAll(principal.getCommunityId());
    }

    @GetMapping("/open")
    public List<PaymentDispute> open(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listOpen(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<PaymentDispute> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public PaymentDispute raise(@AuthenticationPrincipal UserPrincipal principal,
                                 @RequestBody PaymentDispute dispute) {
        dispute.setCommunityId(principal.getCommunityId());
        dispute.setUserId(principal.getUserId());
        return service.raise(dispute);
    }

    @PutMapping("/{id}/resolve")
    public PaymentDispute resolve(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        return service.resolve(principal.getCommunityId(), id, body.get("resolution"));
    }
}
