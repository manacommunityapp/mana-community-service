package com.manacommunity.api.noticeboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private String category;
    private String priority;
    private boolean pinned;
    private String expiresOn;
}
