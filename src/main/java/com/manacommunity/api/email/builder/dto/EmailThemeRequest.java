package com.manacommunity.api.email.builder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailThemeRequest(
        Long id,
        @NotNull Long communityId,
        @NotBlank String name,
        @NotBlank String themeJson,
        Boolean isDefault
) {}
