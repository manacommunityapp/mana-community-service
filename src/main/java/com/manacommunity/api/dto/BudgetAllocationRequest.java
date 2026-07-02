package com.manacommunity.api.dto;

import com.manacommunity.api.model.ExpenseCategory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetAllocationRequest {
    private String financialYear;
    private ExpenseCategory category;
    private BigDecimal allocatedAmount;
    private String notes;
}
