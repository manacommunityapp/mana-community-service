package com.manacommunity.api.pins.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePinRequest {

    @NotBlank(message = "Category is required")
    private String category; // ALERTS, SPORTS, HELP_NEEDED, OFFERS, CARPOOL, LOST_FOUND, GENERAL

    @NotBlank(message = "Content is required")
    @Size(max = 280, message = "Content cannot exceed 280 characters")
    private String content;

    private String imageUrl;
    private String externalLink;

    /** Duration in hours (e.g. 6, 12, 24, 48, 168). Default 24 hours. */
    @Builder.Default
    private Integer durationHours = 24;
}
