package com.manacommunity.api.finance.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInstallmentDto {
    private String id;
    private String name;
    private BigDecimal totalAmount;
    private BigDecimal monthlyEmi;
    private BigDecimal interestRate;
    private Integer totalTenorMonths;
    private Integer remainingTenorMonths;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private Integer percentPaid;
    private LocalDate startDate;
    private LocalDate nextDueDate;
    private String accountId;
    private String accountName;
    private String categoryId;
    private String categoryName;
    private Boolean isAutoDeduct;
    private String status;
}
