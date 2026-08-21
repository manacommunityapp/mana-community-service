package com.manacommunity.api.notification.provider;

public interface SmsProvider {

    SmsSendResponse send(SmsSendRequest request);

    /** Returns latest delivery status from provider (for polling-based tracking). */
    String getDeliveryStatus(String providerMessageId);

    boolean isHealthy();

    String getProviderName();
}
