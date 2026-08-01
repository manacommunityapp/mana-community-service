package com.manacommunity.api.cfbos.accounting.dto;

import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountGroupDto {
    private Long id;
    private String code;
    private String name;
    private AccountType accountType;
    private Long parentGroupId;
    private Integer displayOrder;
}
