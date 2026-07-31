package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroceryStoreRequest {
    @NotBlank
    private String name;
    private String description;
    private String address;
    private String logoUrl;
    private String coverImageUrl;
    private String storeType;
    private Boolean deliveryEnabled;
    private BigDecimal minOrder;
    private BigDecimal deliveryFee;
}
