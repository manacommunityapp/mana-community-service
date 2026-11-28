package com.manacommunity.api.notification.router.channels;

import com.manacommunity.api.notification.router.DeliveryResult;
import com.manacommunity.api.notification.router.NotificationChannel;
import com.manacommunity.api.notification.router.NotificationRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class PushNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() { return "PUSH"; }

    @Override
    public int priorityOrder() { return 1; }

    @Override
    public boolean supports(NotificationRequest request) {
        return request.recipientId() != null;
    }

    @Override
    public DeliveryResult send(NotificationRequest request) {
        long start = System.currentTimeMillis();
        try {
            // In production: FCM/APNs push via Firebase Admin SDK
            String msgId = "push-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("PUSH sent to user {}: {}", request.recipientId(), request.title());
            return DeliveryResult.success(channelId(), msgId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return DeliveryResult.failure(channelId(), e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}
