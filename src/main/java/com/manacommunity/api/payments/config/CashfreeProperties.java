package com.manacommunity.api.payments.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for the {@code app.cashfree} configuration block.
 * All secrets are injected from environment variables — never hardcoded.
 *
 * <pre>
 * app:
 *   cashfree:
 *     enabled: ${CASHFREE_ENABLED:false}
 *     app-id: ${CASHFREE_APP_ID:}
 *     secret-key: ${CASHFREE_SECRET_KEY:}
 *     webhook-secret: ${CASHFREE_WEBHOOK_SECRET:}
 *     base-url: ${CASHFREE_BASE_URL:https://sandbox.cashfree.com/pg}
 *     api-version: ${CASHFREE_API_VERSION:2022-09-01}
 *     currency: ${CASHFREE_CURRENCY:INR}
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cashfree")
public class CashfreeProperties {

    /** Master switch — mirrors the @ConditionalOnProperty gate. */
    private boolean enabled = false;

    /** Cashfree App ID (publishable). */
    private String appId = "";

    /** Cashfree secret key. NEVER expose to the frontend. */
    private String secretKey = "";

    /** Webhook secret (same as secretKey per Cashfree docs for signature verification). */
    private String webhookSecret = "";

    /** Cashfree PG API base URL. Sandbox: https://sandbox.cashfree.com/pg */
    private String baseUrl = "https://sandbox.cashfree.com/pg";

    /** Cashfree API version header value. */
    private String apiVersion = "2022-09-01";

    /** ISO 4217 currency code. Default is INR. */
    private String currency = "INR";
}
