package com.manacommunity.api.finance.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalAccountDto {
    @NotBlank(message = "Account name is required")
    private String name;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "SAVINGS|CHECKING|CREDIT_CARD|CASH|INVESTMENT|LOAN", message = "Type must be SAVINGS, CHECKING, CREDIT_CARD, CASH, INVESTMENT, or LOAN")
    private String type;

    @NotNull(message = "Initial balance is required")
    private BigDecimal balance;

    private BigDecimal creditLimit;
    private String currency;
    private String bankName;
    private String accountNumber;
    private Integer billingDay;
    private Integer paymentDueDay;
    private String color;
    private String icon;
    private Boolean isActive;
}
