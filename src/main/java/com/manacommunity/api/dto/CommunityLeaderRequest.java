package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommunityLeaderRequest(
        @NotNull Long userId,
        @NotBlank String designation,
        String committee,
        String contactPhone,
        String contactEmail,
        Integer displayOrder
) {}
