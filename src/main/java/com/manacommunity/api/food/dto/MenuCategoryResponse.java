package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuCategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean active;
    private Long parentId;
    private Integer itemCount;
}
