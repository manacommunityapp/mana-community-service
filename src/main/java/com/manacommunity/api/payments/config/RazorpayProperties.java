package com.manacommunity.api.payments.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for the {@code app.razorpay} configuration block.
 * All secrets are injected from environment variables — never hardcoded.
 *
 * <pre>
 * app:
 *   razorpay:
 *     enabled: ${RAZORPAY_ENABLED:false}
 *     key-id: ${RAZORPAY_KEY_ID:}
 *     key-secret: ${RAZORPAY_KEY_SECRET:}
 *     webhook-secret: ${RAZORPAY_WEBHOOK_SECRET:}
 *     currency: ${RAZORPAY_CURRENCY:INR}
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.razorpay")
public class RazorpayProperties {

    /** Master switch — mirrors the @ConditionalOnProperty gate. */
    private boolean enabled = false;

    /** Razorpay publishable key (safe to send to the frontend for the checkout modal). */
    private String keyId = "";

    /** Razorpay secret key. NEVER expose to the frontend. */
    private String keySecret = "";

    /** Webhook secret used for HMAC-SHA256 verification of incoming webhook events. */
    private String webhookSecret = "";

    /** ISO 4217 currency code. Default is INR. */
    private String currency = "INR";
}
