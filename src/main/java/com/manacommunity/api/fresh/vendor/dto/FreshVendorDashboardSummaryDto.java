package com.manacommunity.api.fresh.vendor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshVendorDashboardSummaryDto {
    private Long vendorId;
    private String businessName;
    private List<VendorCapabilityDto> approvedCapabilities;
    private int todayOrdersCount;
    private int tomorrowDemandUnits;
    private int activeBatchesCount;
    private int qualityPendingCount;
    private int dispatchPendingCount;
    private BigDecimal monthToDateSales;
    private BigDecimal settlementDueAmount;
    private List<String> activeAlerts;
}
