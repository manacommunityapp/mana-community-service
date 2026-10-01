package com.manacommunity.api.cfbos.statements.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class BalanceSheetDto {
    private LocalDate asOfDate;
    private Map<String, BigDecimal> currentAssets;
    private Map<String, BigDecimal> nonCurrentAssets;
    private BigDecimal totalAssets;

    private Map<String, BigDecimal> currentLiabilities;
    private Map<String, BigDecimal> longTermReserves;
    private BigDecimal totalLiabilities;

    private BigDecimal accumulatedSurplus;
    private BigDecimal totalEquityAndLiabilities;
    private boolean isBalanced;
}
