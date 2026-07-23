package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VendorCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Boolean isActive;
    private List<VendorCategoryResponse> children;
}
