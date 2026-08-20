package com.manacommunity.api.dto;

import com.manacommunity.api.model.PostType;
import com.manacommunity.api.model.PostVisibility;
import com.manacommunity.api.model.PostPriority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public record PostRequest(
    @NotBlank String content,
    String title,
    String imageUrl,
    PostType type,
    PostVisibility visibility,
    PostPriority priority,
    Long groupId,
    Double price,
    String location,
    String pollQuestion,
    String pollOptions,
    LocalDateTime pollEndDate,
    Boolean pollAnonymous,
    String hashtags,
    String mentions,
    String linkUrl,
    String linkTitle,
    String linkDescription,
    String linkImage,
    LocalDateTime eventDate,
    LocalDateTime eventEndDate,
    String eventVenue,
    List<MediaAttachment> mediaAttachments
) {
    public record MediaAttachment(
        String mediaUrl,
        String mediaType,
        String thumbnailUrl,
        String altText,
        Integer sortOrder,
        /** UUID of the MediaObject — used to regenerate fresh S3/CloudFront URLs at read time. */
        String mediaObjectId
    ) {}
}
