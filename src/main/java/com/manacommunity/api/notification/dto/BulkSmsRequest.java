package com.manacommunity.api.notification.dto;

import com.manacommunity.api.notification.enums.SmsLanguage;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class BulkSmsRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String templateCode;
    private SmsLanguage language = SmsLanguage.EN;
    /** JSON string of recipient filter criteria */
    private String recipientFilter;
    private Map<String, String> templateVariables;
    private LocalDateTime scheduledAt;
    private Long communityId;
}
