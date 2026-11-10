package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsBatchStatusRequest {
    @NotEmpty(message = "eventIds cannot be empty")
    private List<Long> eventIds;

    @NotNull(message = "status is required")
    private String status;
}