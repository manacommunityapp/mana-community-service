package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.PaymentReceipt;
import com.manacommunity.api.payments.service.PaymentReceiptService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments/receipts")
@RequiredArgsConstructor
public class PaymentReceiptController {
    private final PaymentReceiptService service;

    @GetMapping
    public List<PaymentReceipt> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listAll(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<PaymentReceipt> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public PaymentReceipt issue(@AuthenticationPrincipal UserPrincipal principal,
                                 @RequestBody PaymentReceipt receipt) {
        receipt.setCommunityId(principal.getCommunityId());
        return service.issue(receipt);
    }
}
