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

    /**
     * Optional UUID of the MediaObject created by the Media Service upload.
     * When provided, EventGalleryService will look up the MediaObject and
     * use MediaUrlService to regenerate fresh presigned/CloudFront URLs at
     * read time instead of storing the expiring presigned URL.
     */
    private String mediaId;
}
