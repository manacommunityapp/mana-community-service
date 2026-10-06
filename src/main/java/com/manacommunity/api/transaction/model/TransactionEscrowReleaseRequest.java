package com.manacommunity.api.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEscrowReleaseRequest {
    private String transactionNumber;
    private String reason;
    private String handoverPassCode;
}
