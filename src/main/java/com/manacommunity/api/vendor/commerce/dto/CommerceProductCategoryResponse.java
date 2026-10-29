package com.manacommunity.api.vendor.commerce.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommerceProductCategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String icon;
    private String description;
    private Long parentId;
    private String parentName;
    private Boolean active;
    private Integer displayOrder;
    private List<CommerceProductCategoryResponse> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
