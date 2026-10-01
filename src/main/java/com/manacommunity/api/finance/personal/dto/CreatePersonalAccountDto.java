package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalAccountDto {
    private String name;
    private String type;
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
