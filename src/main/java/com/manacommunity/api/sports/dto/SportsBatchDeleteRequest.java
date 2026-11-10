package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsBatchDeleteRequest {
    @NotEmpty(message = "eventIds cannot be empty")
    private List<Long> eventIds;
}