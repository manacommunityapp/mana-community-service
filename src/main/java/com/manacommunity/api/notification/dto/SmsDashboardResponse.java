package com.manacommunity.api.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SmsDashboardResponse {
    private long totalSent;
    private long totalDelivered;
    private long totalFailed;
    private long queued;
    private long dlq;
    private long retrying;
    private BigDecimal costToday;
    private BigDecimal costThisMonth;
    private boolean providerHealthy;
    private String providerName;
}
