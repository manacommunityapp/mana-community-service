package com.manacommunity.api.payments.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the {@link RazorpayClient} bean only when {@code app.razorpay.enabled=true}.
 * When the flag is false (default in application.yaml) the entire payment module
 * stays dormant: no beans are created, no credentials are required, the controller
 * returns 404 for every request.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RazorpayClientConfig {

    private final RazorpayProperties properties;

    @Bean
    @ConditionalOnProperty(name = "app.razorpay.enabled", havingValue = "true")
    public RazorpayClient razorpayClient() throws RazorpayException {
        String keyPreview = properties.getKeyId().length() > 12
                ? properties.getKeyId().substring(0, 12) + "..."
                : "***";
        log.info("Initialising Razorpay client (keyId={})", keyPreview);
        return new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
    }
}
