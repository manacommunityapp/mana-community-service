package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HomeChefMenuRequest {
    @NotBlank
    private String name;
    private String description;
    private String imageUrl;
    @NotNull
    private BigDecimal price;
    private String category;
    private Boolean isVeg;
    private Integer calories;
    private BigDecimal protein;
    private Integer preparationTime;
    private String availableDays;
    private String orderBeforeTime;
    private Integer maxQuantity;
}
