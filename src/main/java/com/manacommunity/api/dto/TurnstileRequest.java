package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TurnstileRequest(
    @NotBlank String name,
    String location,
    @NotNull String type
) {}
