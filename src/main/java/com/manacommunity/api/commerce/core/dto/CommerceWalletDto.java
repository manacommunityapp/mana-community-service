package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class CommerceWalletDto {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal balance;
    private String currency;
    private String status;
    private List<WalletTxnDto> transactions;

    @Data @Builder
    public static class WalletTxnDto {
        private Long id;
        private String txnRef;
        private String txnType;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String referenceId;
        private String description;
        private String createdAt;
    }
}
