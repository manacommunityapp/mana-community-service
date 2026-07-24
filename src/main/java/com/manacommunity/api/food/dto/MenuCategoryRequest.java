package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuCategoryRequest {
    @NotBlank
    private String name;
    private String description;
    private String imageUrl;
    private Integer sortOrder;
    private Long parentId;
    private Boolean active;
}
