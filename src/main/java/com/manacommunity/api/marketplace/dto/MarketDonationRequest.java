package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketDonation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDonationRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private MarketDonation.ItemCondition condition;

    private MarketDonation.SharingMode sharingMode;

    private String barterPreferredItem;

    private String imageUrl;
}
