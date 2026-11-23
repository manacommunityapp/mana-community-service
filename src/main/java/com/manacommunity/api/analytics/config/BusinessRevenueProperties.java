package com.manacommunity.api.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mana.business")
public class BusinessRevenueProperties {
    /** Default settlement SLA; override with mana.business.vendor-settlement-sla-days. */
    private Integer vendorSettlementSlaDays = 7;

    public Integer getVendorSettlementSlaDays() { return vendorSettlementSlaDays; }
    public void setVendorSettlementSlaDays(Integer vendorSettlementSlaDays) {
        this.vendorSettlementSlaDays = vendorSettlementSlaDays;
    }
}
