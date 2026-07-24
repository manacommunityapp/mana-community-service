package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceCategoryRequest {

    @NotBlank
    private String name;

    private String icon;
    private String color;
    private String description;
    private Integer displayOrder;
    private String status;
    private String imageUrl;
}
