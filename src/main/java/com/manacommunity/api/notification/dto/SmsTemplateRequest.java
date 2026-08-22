package com.manacommunity.api.notification.dto;

import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsTemplateRequest {
    @NotBlank
    private String templateCode;
    @NotBlank
    private String name;
    @NotBlank
    private String body;
    @NotNull
    private SmsLanguage language;
    @NotNull
    private MessageType messageType;
    private String dltTemplateId;
    private boolean unicode;
}
