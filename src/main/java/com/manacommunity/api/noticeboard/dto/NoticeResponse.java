package com.manacommunity.api.noticeboard.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {
    private Long id;
    private String title;
    private String body;
    private String category;
    private String priority;
    private String targetAudience;
    private String targetBlock;
    private boolean pinned;
    private boolean requiresAcknowledgement;
    private boolean isAcknowledgedByCurrentUser;
    private boolean isReadByCurrentUser;
    private String attachments;
    private String status;
    private String expiresOn;
    private String scheduledPublishAt;
    private Long authorId;
    private String authorName;
    private Long communityId;
    private String createdAt;
    private String updatedAt;
}
