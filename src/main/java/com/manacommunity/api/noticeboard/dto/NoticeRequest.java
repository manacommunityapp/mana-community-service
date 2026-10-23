package com.manacommunity.api.noticeboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    private String category;
    private String priority;
    private String targetAudience;
    private String targetBlock;
    private boolean pinned;
    private boolean requiresAcknowledgement;
    private String attachments;
    private String expiresOn;
    private String scheduledPublishAt;
}
