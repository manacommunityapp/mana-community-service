package com.manacommunity.api.cfbos.wallet.dto;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WalletTransactionDto {
    private Long id;
    private Long walletId;
    private WalletTransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceType;
    private Long referenceId;
    private String narration;
    private LocalDateTime createdAt;
}
