package com.manacommunity.api.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DemandItemRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String category;
}
