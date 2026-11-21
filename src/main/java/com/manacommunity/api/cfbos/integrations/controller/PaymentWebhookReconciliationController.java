package com.manacommunity.api.cfbos.integrations.controller;

import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// NOTE: This controller handles inbound payment gateway webhooks.
// In addition to the @PreAuthorize below, production deployments should restrict
// access to known PG IP ranges at the WAF/load-balancer/firewall level
// (e.g. Razorpay: 15.206.33.56/32, 13.235.210.230/32; Stripe: 3.18.12.63/32 etc.)
@RestController
@RequestMapping({"/api/finance/webhooks", "/api/payments/webhooks", "/api/v1/payments/webhooks"})
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('Admin') or hasAuthority('System')")
public class PaymentWebhookReconciliationController {

    private final PaymentWebhookReconciliationService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpay(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        webhookService.processRazorpayWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> handleStripe(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        webhookService.processStripeWebhook(rawPayload, signature);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}
