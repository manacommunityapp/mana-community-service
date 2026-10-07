package com.manacommunity.api.transactioncore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowReleaseRequest {

    @NotBlank(message = "Intent ID or Escrow ID is required")
    private String intentId;

    private String verificationCode;
    private String verifiedBy;
    private BigDecimal releaseAmount;
    private List<PaymentSplit> finalSplits;
    private String releaseReason;
}
