package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CommerceLoyaltyDto {
    Long accountId;
    Long availablePoints;
    Long totalPointsEarned;
    Long totalPointsSpent;
    String tier;
    List<TxnDto> recentTransactions;

    @Value
    @Builder
    public static class TxnDto {
        Long id;
        String type;
        Long points;
        Long balanceAfter;
        String description;
        String orderNumber;
        String createdAt;
    }
}
