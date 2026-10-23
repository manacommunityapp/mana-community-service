package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketDonation;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDonationResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private MarketDonation.ItemCondition condition;
    private String imageUrl;
    private MarketDonation.SharingMode sharingMode;
    private String barterPreferredItem;
    private MarketDonation.DonationStatus status;
    private String donorName;
    private Long donorId;
    private Long communityId;
    private String claimedByName;
    private Long claimedById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
