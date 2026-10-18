package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class DriverEarningsSummaryDto {
    private BigDecimal todayEarnings;
    private BigDecimal weekEarnings;
    private BigDecimal monthEarnings;
    private BigDecimal pendingSettlement;
    private int todayTrips;
    private int totalTrips;
}
