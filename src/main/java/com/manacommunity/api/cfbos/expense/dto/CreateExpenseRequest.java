package com.manacommunity.api.cfbos.expense.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateExpenseRequest {
    private Long expenseCategoryId;
    private LocalDate expenseDate;
    private String description;
    private String paymentMode;
    private String paymentReference;
    private Long vendorId;
    private Long costCenterId;
    private Long fundId;
    private Long submittedBy;
    private List<ExpenseLineItemRequest> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExpenseLineItemRequest {
        private String description;
        private Long accountId;
        private BigDecimal quantity;
        private BigDecimal rate;
        private String hsnSacCode;
        private Boolean isTaxable;
    }
}
