package com.manacommunity.api.db.optimization.wallet.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAccountDto {

    private Long walletId;
    private Long residentId;
    private BigDecimal balance;
    private long version;
    private String status; // ACTIVE, FROZEN
}
