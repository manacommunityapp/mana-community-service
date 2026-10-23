package com.manacommunity.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroceryItemResponse {
    private Long id;
    private String name;
    private String category;
    private String unit;
    private BigDecimal pricePerUnit;
    private Boolean available;
    private String imageUrl;
    private String vendorName;
    private LocalDateTime createdAt;
}
