package com.manacommunity.api.unit.notification;

import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.webhook.TwilioWebhookVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TwilioWebhookVerifierTest {

    private TwilioWebhookVerifier verifier(String authToken) {
        SmsProperties props = new SmsProperties();
        props.setAuthToken(authToken);
        return new TwilioWebhookVerifier(props);
    }

    @Test
    @DisplayName("blank auth token → verification bypassed (returns true)")
    void blankTokenBypassesVerification() {
        TwilioWebhookVerifier v = verifier("");
        boolean result = v.verify("any-sig", "https://example.com/webhook", Map.of("k", "v"));
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("null auth token → verification bypassed (returns true)")
    void nullTokenBypassesVerification() {
        TwilioWebhookVerifier v = verifier(null);
        boolean result = v.verify(null, "https://example.com/webhook", Map.of());
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("whitespace-only auth token → verification bypassed (returns true)")
    void whitespaceTokenBypassesVerification() {
        TwilioWebhookVerifier v = verifier("   ");
        boolean result = v.verify("sig", "https://example.com/webhook", Map.of("MessageSid", "SM123"));
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("real auth token with wrong signature → returns false")
    void wrongSignatureReturnsFalse() {
        // Use a plausible but fake auth token — Twilio will reject the HMAC
        TwilioWebhookVerifier v = verifier("AC0000000000000000000000000000000000");
        boolean result = v.verify("wrong-signature", "https://myapp.com/webhook",
                Map.of("MessageSid", "SM123", "MessageStatus", "delivered"));
        assertThat(result).isFalse();
    }
}
