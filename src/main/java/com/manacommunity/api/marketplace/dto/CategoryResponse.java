package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String icon;
    private Long parentId;
    private String parentName;
    private int sortOrder;
    private boolean active;
}
