package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteEngagementRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal hoursWorked;

    @DecimalMin("0.0")
    private BigDecimal amountDue;

    private String termsSummary;
}
