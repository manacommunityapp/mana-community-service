package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketListing;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketListingRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    private String priceUnit;

    @NotBlank(message = "Category is required")
    private String category;

    private MarketListing.Condition condition;

    private String warranty;

    private MarketListing.TransactionMode transactionMode;

    private MarketListing.ListingVisibility visibility;

    private String location;

    private List<String> imageUrls;
}
