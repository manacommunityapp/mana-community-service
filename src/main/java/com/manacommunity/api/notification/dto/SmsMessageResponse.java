package com.manacommunity.api.notification.dto;

import com.manacommunity.api.notification.enums.SmsStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SmsMessageResponse {
    private Long id;
    private String phoneNumber;
    private String templateCode;
    private String renderedBody;
    private SmsStatus status;
    private String providerMessageId;
    private int retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private String failureReason;
}
