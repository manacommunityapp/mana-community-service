package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityDesignationRequest(
        @NotBlank(message = "Designation name is required")
        @Size(max = 100, message = "Designation name must not exceed 100 characters")
        String name,
        Integer displayOrder
) {}
