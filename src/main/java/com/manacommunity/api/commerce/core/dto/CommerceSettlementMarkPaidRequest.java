package com.manacommunity.api.commerce.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommerceSettlementMarkPaidRequest {
    /** Bank UTR / UPI reference for the payout. */
    @NotBlank
    private String payoutUtr;

    private String adminNote;
}
