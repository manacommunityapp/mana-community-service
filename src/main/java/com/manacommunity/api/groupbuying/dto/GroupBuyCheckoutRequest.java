package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyCheckoutRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    private String paymentMethod;
    private BigDecimal amountToPayNow;
    private BigDecimal escrowHoldAmount;
    private String deliveryAddressOrPickup;
    private String specialNotes;
}
