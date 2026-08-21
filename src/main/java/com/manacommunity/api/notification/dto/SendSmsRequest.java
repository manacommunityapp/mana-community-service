package com.manacommunity.api.notification.dto;

import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.SmsPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Builder
@Jacksonized
public class SendSmsRequest {
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String templateCode;
    private Map<String, String> variables;
    @Builder.Default
    private SmsLanguage language = SmsLanguage.EN;
    @Builder.Default
    private SmsPriority priority = SmsPriority.NORMAL;
    @Builder.Default
    private MessageType messageType = MessageType.TRANSACTIONAL;
    private Long userId;
    private String referenceType;
    private Long referenceId;
    private String idempotencyKey;
}
