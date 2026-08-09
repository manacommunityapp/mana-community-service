package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventGalleryItemRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String url;

    private String thumbnailUrl;
    private String mediaType;
    private String albumName;
    private String dayTag;
    private String category;
    private String caption;
    private boolean featured;
    private Integer sortOrder;
}
