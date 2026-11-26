package com.manacommunity.api.notification.router.channels;

import com.manacommunity.api.notification.router.DeliveryResult;
import com.manacommunity.api.notification.router.NotificationChannel;
import com.manacommunity.api.notification.router.NotificationRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class VoipIntercomChannel implements NotificationChannel {

    @Override
    public String channelId() { return "VOIP_INTERCOM"; }

    @Override
    public int priorityOrder() { return 2; }

    @Override
    public boolean supports(NotificationRequest request) {
        return request.isUrgent() && "VISITOR_ARRIVAL".equals(request.category());
    }

    @Override
    public DeliveryResult send(NotificationRequest request) {
        long start = System.currentTimeMillis();
        try {
            String msgId = "voip-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("VOIP e-Intercom call to user {}: {}", request.recipientId(), request.title());
            return DeliveryResult.success(channelId(), msgId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return DeliveryResult.failure(channelId(), e.getMessage(), System.currentTimeMillis() - start);
        }
    }
}
