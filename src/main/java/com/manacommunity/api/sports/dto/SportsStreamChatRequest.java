package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SportsStreamChatRequest {
    @NotBlank
    @Size(max = 500)
    private String text;

    /** Set to true when posted by the organiser as official commentary */
    private boolean commentary;
}
