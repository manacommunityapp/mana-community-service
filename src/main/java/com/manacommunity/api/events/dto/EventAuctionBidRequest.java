package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventAuctionBidRequest {

    @NotNull
    private Long itemId;

    @NotNull
    @Positive
    private BigDecimal amount;
}
