package com.manacommunity.api.cfbos.statements.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class CashFlowStatementDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal netCashFromOperating;
    private Map<String, BigDecimal> operatingDetails;

    private BigDecimal netCashFromInvesting;
    private Map<String, BigDecimal> investingDetails;

    private BigDecimal netCashFromFinancing;
    private Map<String, BigDecimal> financingDetails;

    private BigDecimal openingCashBalance;
    private BigDecimal closingCashBalance;
    private BigDecimal netChangeInCash;
}
