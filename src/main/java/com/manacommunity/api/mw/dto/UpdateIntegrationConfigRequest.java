package com.manacommunity.api.mw.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class UpdateIntegrationConfigRequest {
    private Boolean freshEnabled;
    private Boolean homesEnabled;
    private Boolean sportsEnabled;
    private BigDecimal baseDeliveryPay;
    private BigDecimal perKmRate;
    private BigDecimal onTimeBonus;
}
