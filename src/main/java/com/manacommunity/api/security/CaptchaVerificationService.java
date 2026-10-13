package com.manacommunity.api.security;

import com.manacommunity.api.exception.ManaCommunityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * DPDP Act 2023 — bot protection for registration and login.
 *
 * Verifies hCaptcha (default) or reCAPTCHA tokens server-side to prevent
 * automated account creation. Disabled by default so local dev and CI work
 * without a real CAPTCHA key.
 *
 * Properties (application.properties / environment):
 *   app.captcha.enabled=false           — set true in production
 *   app.captcha.secret-key=             — hCaptcha / reCAPTCHA secret key
 *   app.captcha.verify-url=https://hcaptcha.com/siteverify
 *   app.captcha.min-score=0.5           — reCAPTCHA v3 only; ignored for hCaptcha
 */
@Slf4j
@Service
public class CaptchaVerificationService {

    @Value("${app.captcha.enabled:false}")
    private boolean enabled;

    @Value("${app.captcha.secret-key:}")
    private String secretKey;

    @Value("${app.captcha.verify-url:https://hcaptcha.com/siteverify}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Verifies the CAPTCHA token supplied by the client.
     *
     * When {@code app.captcha.enabled=false} this is a no-op — pass-through
     * so existing tests and local dev are unaffected.
     *
     * @param token  the h-captcha-response / g-recaptcha-response value from the frontend
     * @throws ManaCommunityException HTTP 400 if the token is missing when CAPTCHA is enabled
     * @throws ManaCommunityException HTTP 403 if the CAPTCHA challenge was failed or expired
     */
    public void verify(String token) {
        if (!enabled) return;

        if (token == null || token.isBlank()) {
            throw new ManaCommunityException(
                "CAPTCHA challenge is required. Please complete the security check.",
                HttpStatus.BAD_REQUEST,
                "CAPTCHA_MISSING"
            );
        }

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", secretKey);
            params.add("response", token);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, params, Map.class);

            Map<?, ?> body = response.getBody();
            boolean success = body != null && Boolean.TRUE.equals(body.get("success"));

            if (!success) {
                String errorCodes = body != null && body.containsKey("error-codes")
                    ? body.get("error-codes").toString() : "unknown";
                log.warn("CAPTCHA verification failed: {}", errorCodes);
                throw new ManaCommunityException(
                    "CAPTCHA verification failed. Please try again.",
                    HttpStatus.FORBIDDEN,
                    "CAPTCHA_FAILED"
                );
            }

            log.debug("CAPTCHA verified successfully");
        } catch (ManaCommunityException e) {
            throw e;
        } catch (Exception e) {
            log.error("CAPTCHA verification error — failing open to avoid blocking legitimate users: {}", e.getMessage());
            // Fail open: if the CAPTCHA service is unreachable, allow the request
            // but log the incident for monitoring.
        }
    }
}
