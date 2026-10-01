package com.manacommunity.api.cfbos.wallet.dto;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TopupWalletRequest {
    private Long residentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String gatewayReference;
    private String narration;
}
