package com.manacommunity.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 60, message = "Name must not exceed 60 characters")
    private String name;

    @Size(max = 60, message = "Icon name must not exceed 60 characters")
    private String icon;

    private Long parentId;

    private Integer sortOrder;
}
