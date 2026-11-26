package com.manacommunity.api.notification.router.channels;

import com.manacommunity.api.notification.router.DeliveryResult;
import com.manacommunity.api.notification.router.NotificationChannel;
import com.manacommunity.api.notification.router.NotificationRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class WhatsAppChannel implements NotificationChannel {

    @Override
    public String channelId() { return "WHATSAPP"; }

    @Override
    public int priorityOrder() { return 3; }

    @Override
    public boolean supports(NotificationRequest request) {
        return request.recipientPhone() != null && !request.recipientPhone().isBlank();
    }

    @Override
    public DeliveryResult send(NotificationRequest request) {
        long start = System.currentTimeMillis();
        try {
            String msgId = "wa-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("WhatsApp to {}: {}", request.recipientPhone(), request.title());
            return DeliveryResult.success(channelId(), msgId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return DeliveryResult.failure(channelId(), e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}
