package com.manacommunity.api.cfbos.accounting.dto;

import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountTreeNodeDto {
    private Long id;
    private String code;
    private String name;
    private AccountType accountType;
    private BigDecimal currentBalance;
    private Boolean isSystemAccount;
    private List<AccountTreeNodeDto> children;
}
