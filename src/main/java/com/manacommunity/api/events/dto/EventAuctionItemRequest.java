package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventAuctionItemRequest {

    private Long eventId;

    @NotBlank
    private String name;

    private String description;
    private String category;

    @NotNull
    @Positive
    private BigDecimal basePrice;

    private BigDecimal minIncrement;

    private String imageEmoji;
    private String imageUrl;

    /** UPCOMING | LIVE | CLOSED */
    private String status;

    private Integer sortOrder;
}
