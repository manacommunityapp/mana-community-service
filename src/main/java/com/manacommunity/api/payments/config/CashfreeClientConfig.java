package com.manacommunity.api.payments.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Provides the RestTemplate used exclusively for Cashfree PG API calls.
 * A dedicated bean keeps timeouts and error handling isolated from
 * other HTTP clients in the application.
 */
@Configuration
@ConditionalOnProperty(name = "app.cashfree.enabled", havingValue = "true")
public class CashfreeClientConfig {

    @Bean("cashfreeRestTemplate")
    public RestTemplate cashfreeRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
