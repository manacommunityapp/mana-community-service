package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PantryItemRequest {
    @NotBlank
    private String name;
    private String category;
    @NotNull
    private Double quantity;
    private String unit;
    private LocalDate expiryDate;
    private Double lowStockThreshold;
}
