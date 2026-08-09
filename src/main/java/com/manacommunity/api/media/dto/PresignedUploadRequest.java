package com.manacommunity.api.media.dto;

import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for obtaining a presigned S3 PUT URL.
 * The client uses the returned URL to upload the file directly to S3
 * (bypassing the app server) and then calls confirm endpoint.
 */
@Data
public class PresignedUploadRequest {

    @NotNull(message = "module is required")
    private MediaModule module;

    @NotBlank(message = "moduleId is required")
    private String moduleId;

    @NotNull(message = "communityId is required")
    private Long communityId;

    private String subContext;

    @NotNull(message = "mediaType is required")
    private MediaType mediaType;

    /** Client-supplied MIME type hint — validated server-side after upload. */
    @NotBlank(message = "mimeType is required")
    private String mimeType;

    @NotBlank(message = "originalFileName is required")
    private String originalFileName;

    private Long fileSize;
    private boolean approvalRequired = false;
    private boolean featured = false;
    private String caption;
    private String altText;
    private int sortOrder = 0;
}
