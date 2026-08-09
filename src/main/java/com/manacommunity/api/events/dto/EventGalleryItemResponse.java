package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventGalleryItemResponse {

    private Long id;
    private Long eventId;
    private String url;
    private String thumbnailUrl;
    private String mediaType;
    private String albumName;
    private String dayTag;
    private String category;
    private String caption;
    private boolean featured;
    private int sortOrder;
    private String uploadedByName;
    private String createdAt;
}
