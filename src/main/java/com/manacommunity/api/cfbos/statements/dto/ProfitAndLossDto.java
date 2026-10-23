package com.manacommunity.api.cfbos.statements.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class ProfitAndLossDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, BigDecimal> revenueBreakdown;
    private BigDecimal totalRevenue;

    private Map<String, BigDecimal> expenseBreakdown;
    private BigDecimal totalExpenses;

    private BigDecimal netSurplusOrDeficit;
    private Double operatingMarginPercent;
}
