package com.manacommunity.api.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ListingRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private BigDecimal price;

    private String priceUnit;

    @NotBlank
    private String category;

    private String transactionMode;
    private String visibility;
    private String location;
    private List<String> imageUrls;
}
