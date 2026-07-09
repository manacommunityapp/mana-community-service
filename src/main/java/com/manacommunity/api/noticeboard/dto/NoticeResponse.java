package com.manacommunity.api.noticeboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String body;
    private String category;
    private String priority;
    private boolean pinned;
    private String expiresOn;
    private Long authorId;
    private String authorName;
    private Long communityId;
    private String createdAt;
    private String updatedAt;
}
