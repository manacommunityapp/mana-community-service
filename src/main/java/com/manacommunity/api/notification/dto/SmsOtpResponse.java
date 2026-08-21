package com.manacommunity.api.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SmsOtpResponse {
    private boolean success;
    private String maskedPhone;
    private LocalDateTime expiresAt;
    private String message;
}
