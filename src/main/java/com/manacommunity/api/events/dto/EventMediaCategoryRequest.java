package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventMediaCategoryRequest {

    @NotBlank
    private String name;

    private Integer sortOrder;
}
