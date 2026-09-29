package com.manacommunity.api.homeservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class MonthlyBillCalculationResult {
    private int totalScheduledDays;
    private int completedDays;
    private int absentDays;
    private int leaveDays;
    private BigDecimal basePrice;
    private BigDecimal totalBill;
    private String pricingModel;
    private String formulaSummary;
}
