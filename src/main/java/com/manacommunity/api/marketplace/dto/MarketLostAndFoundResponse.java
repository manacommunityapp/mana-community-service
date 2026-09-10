package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketLostAndFound;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketLostAndFoundResponse {

    private Long id;
    private String title;
    private String description;
    private MarketLostAndFound.PostType type;
    private String category;
    private String imageUrl;
    private String location;
    private LocalDate dateOccurred;
    private MarketLostAndFound.LostFoundStatus status;
    private String reporterName;
    private Long reporterId;
    private Long communityId;
    private String claimedByName;
    private Long claimedById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
