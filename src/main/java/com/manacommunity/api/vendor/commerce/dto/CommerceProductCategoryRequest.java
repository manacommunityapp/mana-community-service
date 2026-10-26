package com.manacommunity.api.vendor.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommerceProductCategoryRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 120)
    private String slug;

    @Size(max = 100)
    private String icon;

    @Size(max = 500)
    private String description;

    private Long parentId;

    private Integer displayOrder;

    private Boolean active;
}
