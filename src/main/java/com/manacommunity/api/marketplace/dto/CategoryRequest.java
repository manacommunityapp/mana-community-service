package com.manacommunity.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    private String name;

    private String icon;

    private Long parentId;

    private Integer sortOrder;
}
