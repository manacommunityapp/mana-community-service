package com.manacommunity.api.cfbos.integrations.controller;

import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/finance/webhooks", "/api/payments/webhooks", "/api/v1/payments/webhooks"})
@RequiredArgsConstructor
public class PaymentWebhookReconciliationController {

    private final PaymentWebhookReconciliationService webhookService;

    @PostMapping("/cashfree")
    public ResponseEntity<Map<String, String>> handleCashfree(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature,
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp) {
        webhookService.processCashfreeWebhook(payload, signature, timestamp);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }

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
