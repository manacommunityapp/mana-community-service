package com.manacommunity.api.payments.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DailyTrendPoint {
    private String date;
    private long count;
    private BigDecimal totalAmount;
}
