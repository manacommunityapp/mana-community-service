package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteEngagementRequest {

    @NotNull
    private BigDecimal hoursWorked;

    private BigDecimal amountDue;
    private String termsSummary;
}
