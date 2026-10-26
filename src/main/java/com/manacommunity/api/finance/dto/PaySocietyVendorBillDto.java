package com.manacommunity.api.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaySocietyVendorBillDto {

    @NotNull
    private BigDecimal amount;

    private String paymentMethod;

    private String utrReference;

    private String notes;
}
