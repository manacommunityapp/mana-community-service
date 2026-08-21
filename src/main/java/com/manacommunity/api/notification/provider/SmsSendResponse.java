package com.manacommunity.api.notification.provider;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SmsSendResponse {
    private boolean success;
    private String providerMessageId;
    private String status;
    private String rawResponse;
    private String errorCode;
    private String errorMessage;
}
