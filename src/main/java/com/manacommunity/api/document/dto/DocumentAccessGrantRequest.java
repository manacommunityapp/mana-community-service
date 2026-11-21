package com.manacommunity.api.document.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record DocumentAccessGrantRequest(
        @NotBlank String granteeType,
        @NotBlank String granteeId,
        @NotBlank String permission,
        LocalDate expiresAt
) {}
