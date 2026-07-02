package com.manacommunity.api.dto;

import com.manacommunity.api.model.ExpenseCategory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LineItemDto {
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private ExpenseCategory expenseCategory;
    private Boolean createInventoryItem;
    private String assetCategory;
    private String location;
    private String serialNumber;
}
