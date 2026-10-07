package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinDealRequest {
    @NotNull
    @Min(1)
    private Integer quantity;

    private String paymentType;
    private String paymentMethod;
    private String deliveryAddress;
    private String specialNotes;
}
