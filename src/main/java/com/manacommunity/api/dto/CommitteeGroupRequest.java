package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommitteeGroupRequest(
    @NotBlank @Size(max = 100) String name,
    String description,
    Integer displayOrder
) {}
