package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentMandate;
import com.manacommunity.api.payments.service.PaymentMandateService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/mandates")
@RequiredArgsConstructor
public class PaymentMandateController {
    private final PaymentMandateService service;

    @GetMapping
    public List<PaymentMandate> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listActive(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<PaymentMandate> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public PaymentMandate approve(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestBody PaymentMandate mandate) {
        mandate.setCommunityId(principal.getCommunityId());
        mandate.setUserId(principal.getUserId());
        return service.approve(mandate);
    }

    @PutMapping("/{id}/cancel")
    public PaymentMandate cancel(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long id,
                                  @RequestBody Map<String, String> body) {
        return service.cancel(principal.getCommunityId(), id, body.get("reason"));
    }
}
