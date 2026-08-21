package com.manacommunity.api.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSmsPreferenceRequest {
    @NotBlank
    private String notificationType;
    private boolean smsEnabled;
    private boolean whatsappEnabled;
    private String preferredLanguage;
}
