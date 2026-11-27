package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentReward;
import com.manacommunity.api.payments.service.PaymentRewardService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/rewards")
@RequiredArgsConstructor
public class PaymentRewardController {
    private final PaymentRewardService service;

    @GetMapping("/my")
    public List<PaymentReward> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listActive(principal.getCommunityId(), principal.getUserId());
    }

    @GetMapping("/my/balance")
    public Map<String, Object> balance(@AuthenticationPrincipal UserPrincipal principal) {
        return service.balance(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping("/redeem/{id}")
    public PaymentReward redeem(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long id) {
        return service.redeem(principal.getCommunityId(), principal.getUserId(), id);
    }
}
