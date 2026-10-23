package com.manacommunity.api.guard.dto;

import jakarta.validation.constraints.NotBlank;

public record GuardProfileRequest(
        @NotBlank String fullName,
        String phone,
        String employeeId,
        String assignedGate,
        Long userId,
        String notes
) {}
