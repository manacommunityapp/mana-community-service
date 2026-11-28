package com.manacommunity.api.notification.router;

public record DeliveryResult(
        String channelId,
        boolean success,
        String messageId,
        String errorReason,
        long deliveryTimeMs
) {
    public static DeliveryResult success(String channelId, String messageId, long ms) {
        return new DeliveryResult(channelId, true, messageId, null, ms);
    }

    public static DeliveryResult failure(String channelId, String reason, long ms) {
        return new DeliveryResult(channelId, false, null, reason, ms);
    }
}
