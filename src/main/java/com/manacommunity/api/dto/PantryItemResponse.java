package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PantryItemResponse {
    private Long id;
    private String name;
    private String category;
    private Double quantity;
    private String unit;
    private LocalDate expiryDate;
    private Double lowStockThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
