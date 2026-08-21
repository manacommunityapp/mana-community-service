package com.manacommunity.api.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Used in tests and dev when app.sms.provider=MOCK */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "MOCK", matchIfMissing = true)
@Slf4j
public class MockSmsProvider implements SmsProvider {

    @Override
    public SmsSendResponse send(SmsSendRequest request) {
        String mockId = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK SMS] to={} body=\"{}\" id={}", request.getTo(), request.getBody(), mockId);
        return SmsSendResponse.builder()
                .success(true)
                .providerMessageId(mockId)
                .status("sent")
                .rawResponse("mock-response")
                .build();
    }

    @Override
    public String getDeliveryStatus(String providerMessageId) {
        return "delivered";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }
}
