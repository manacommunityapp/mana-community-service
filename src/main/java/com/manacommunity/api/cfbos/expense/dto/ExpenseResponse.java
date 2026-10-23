package com.manacommunity.api.cfbos.expense.dto;
import com.manacommunity.api.cfbos.expense.enums.ExpenseStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String expenseNumber;
    private LocalDate expenseDate;
    private Long expenseCategoryId;
    private String expenseCategoryName;
    private String description;
    private BigDecimal subtotal;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;
    private String paymentMode;
    private String paymentReference;
    private Long vendorId;
    private ExpenseStatus status;
    private Long approvalRequestId;
    private Long journalEntryId;
    private List<ExpenseLineDto> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExpenseLineDto {
        private Long id;
        private String description;
        private Long accountId;
        private BigDecimal quantity;
        private BigDecimal rate;
        private BigDecimal amount;
        private String hsnSacCode;
        private Boolean isTaxable;
    }
}
