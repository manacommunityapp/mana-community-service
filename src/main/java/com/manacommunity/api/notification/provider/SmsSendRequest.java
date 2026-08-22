package com.manacommunity.api.notification.provider;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SmsSendRequest {
    private String to;
    private String body;
    private String from;
    private String idempotencyKey;
}
