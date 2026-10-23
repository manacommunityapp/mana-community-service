package com.manacommunity.api.serviceplatform.amc.dto;

import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmcSubscriptionDto {
    private Long id;
    private Long amcPlanId;
    private String planName;
    private Long userId;
    private String userName;
    private Long communityId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int visitsRemaining;
    private int visitsUsed;
    private AmcSubscriptionStatus status;
    private BigDecimal amountPaid;
}
