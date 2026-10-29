package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CrmEmailCampaignDto {
    private Long id;
    private String subject;
    private String bodyHtml;
    private String filterStage;
    private String filterSource;
    private Long filterTagId;
    private int sentCount;
    private String createdBy;
    private LocalDateTime sentAt;
}
