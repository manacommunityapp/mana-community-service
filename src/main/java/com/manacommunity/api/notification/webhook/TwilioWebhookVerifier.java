package com.manacommunity.api.notification.webhook;

import com.manacommunity.api.notification.config.SmsProperties;
import com.twilio.security.RequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TwilioWebhookVerifier {

    private final SmsProperties smsProperties;

    /**
     * Validates the X-Twilio-Signature header against the request URL + params.
     * Returns true when auth token is not configured (dev / mock mode).
     */
    public boolean verify(String signature, String requestUrl, Map<String, String> params) {
        String authToken = smsProperties.getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            log.warn("Twilio auth token not configured — skipping webhook signature verification");
            return true;
        }
        try {
            RequestValidator validator = new RequestValidator(authToken);
            return validator.validate(requestUrl, params, signature);
        } catch (Exception ex) {
            log.error("Twilio webhook signature verification failed: {}", ex.getMessage());
            return false;
        }
    }
}
