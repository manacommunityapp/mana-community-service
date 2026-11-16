package com.manacommunity.api.cfbos.integrations.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService;
import com.manacommunity.api.payments.config.RazorpayProperties;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/finance/webhooks", "/api/payments/webhooks", "/api/v1/payments/webhooks"})
@RequiredArgsConstructor
public class PaymentWebhookReconciliationController {

    private final PaymentWebhookReconciliationService webhookService;
    private final RazorpayProperties razorpayProperties;
    private final ObjectMapper objectMapper;

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
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        // Validate HMAC-SHA256 signature before trusting any payload content.
        if (signature == null || signature.isBlank()) {
            log.warn("[WEBHOOK][RAZORPAY] Missing X-Razorpay-Signature — rejecting request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing signature"));
        }
        try {
            boolean valid = Utils.verifyWebhookSignature(rawPayload, signature,
                    razorpayProperties.getWebhookSecret());
            if (!valid) {
                log.warn("[WEBHOOK][RAZORPAY] Signature verification failed — rejecting request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid signature"));
            }
        } catch (Exception e) {
            log.error("[WEBHOOK][RAZORPAY] Signature verification threw: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Signature verification error"));
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload,
                    new TypeReference<Map<String, Object>>() {});
            webhookService.processRazorpayWebhook(payload, signature);
        } catch (Exception e) {
            log.error("[WEBHOOK][RAZORPAY] Failed to parse or process payload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
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
