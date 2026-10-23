package com.manacommunity.api.guard.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GuardProfileResponse(
        Long id,
        String fullName,
        String phone,
        String employeeId,
        String assignedGate,
        Long userId,
        String userName,
        String status,
        String notes,
        LocalDateTime createdAt
) {}
