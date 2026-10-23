package com.manacommunity.api.cfbos.statements.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CashflowForecastDto {
    private int horizonDays;
    private BigDecimal currentLiquidBuffer;
    private BigDecimal projectedClosingBuffer;
    private Double collectionEfficiencyTrend;
    private List<ForecastDataPoint> dailyForecasts;
    private List<String> proactiveAlerts;

    @Data
    @Builder
    public static class ForecastDataPoint {
        private LocalDate date;
        private BigDecimal projectedInflow;
        private BigDecimal projectedOutflow;
        private BigDecimal projectedBalance;
    }
}
