package com.manacommunity.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DonationRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String category;

    private String condition;

    private String imageUrl;
}
