package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank String name,
        @NotNull String categoryType,
        String icon,
        String color,
        Integer sortOrder
) {}
