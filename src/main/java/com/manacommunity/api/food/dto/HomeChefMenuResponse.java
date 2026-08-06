package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class HomeChefMenuResponse {
    private Long id;
    private Long chefId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String category;
    private Boolean isVeg;
    private Integer calories;
    private BigDecimal protein;
    private Integer preparationTime;
    private String availableDays;
    private String orderBeforeTime;
    private Integer maxQuantity;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
