package com.manacommunity.api.notification.router;

public interface NotificationChannel {

    String channelId();

    int priorityOrder();

    boolean supports(NotificationRequest request);

    DeliveryResult send(NotificationRequest request);
}
