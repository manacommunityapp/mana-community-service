package com.manacommunity.api.cfbos.wallet.dto;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WalletDto {
    private Long id;
    private Long residentId;
    private BigDecimal balance;
    private BigDecimal totalCredited;
    private BigDecimal totalDebited;
    private Boolean isActive;
}
