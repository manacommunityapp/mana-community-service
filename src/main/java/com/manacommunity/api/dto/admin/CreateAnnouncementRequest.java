package com.manacommunity.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAnnouncementRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must not exceed 120 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;

    /** NORMAL or URGENT */
    private String priority = "NORMAL";

    private boolean pinned = false;

    /** ISO-8601 datetime string, optional */
    private String expiresAt;
}
