package com.manacommunity.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LostAndFoundRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String type;

    private String category;

    private String imageUrl;

    private String location;

    private LocalDate dateOccurred;
}
