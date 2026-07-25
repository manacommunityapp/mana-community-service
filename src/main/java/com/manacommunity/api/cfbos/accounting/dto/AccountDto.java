package com.manacommunity.api.cfbos.accounting.dto;

import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountDto {
    private Long id;
    private String code;
    private String name;
    private AccountType accountType;
    private String accountGroupName;
    private Long accountGroupId;
    private Long parentAccountId;
    private Boolean isSystemAccount;
    private Boolean isBankAccount;
    private Boolean isActive;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private String description;
}
