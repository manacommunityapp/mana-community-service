package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentReconciliation;
import com.manacommunity.api.payments.service.PaymentReconciliationService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments/reconciliation")
@RequiredArgsConstructor
public class PaymentReconciliationController {
    private final PaymentReconciliationService service;

    @GetMapping
    public List<PaymentReconciliation> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.list(principal.getCommunityId());
    }

    @PostMapping
    public PaymentReconciliation create(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody PaymentReconciliation recon) {
        recon.setCommunityId(principal.getCommunityId());
        return service.create(recon);
    }

    @PutMapping("/{id}/approve")
    public PaymentReconciliation approve(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long id) {
        return service.approve(principal.getCommunityId(), id, principal.getUserId());
    }
}
