package com.manacommunity.api.media.dto;

import com.manacommunity.api.media.entity.MediaModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body submitted alongside a multipart file upload (POST /api/media/upload).
 */
@Data
public class MediaUploadRequest {

    @NotNull(message = "module is required")
    private MediaModule module;

    @NotBlank(message = "moduleId is required")
    private String moduleId;

    @NotNull(message = "communityId is required")
    private Long communityId;

    /** Sub-context within the module: gallery, banner, sponsor, volunteer, kyc … */
    private String subContext;

    private String caption;
    private String altText;
    private boolean approvalRequired = false;
    private boolean featured = false;
    private int sortOrder = 0;
}
