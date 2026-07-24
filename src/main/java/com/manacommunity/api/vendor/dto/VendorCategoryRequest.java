package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorCategoryRequest {
    @NotBlank
    private String name;
    private String description;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
}
