package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubmitVendorOfferRequest {
    @NotNull
    private BigDecimal offeredPrice;
    @NotNull
    private Integer minimumQty;
    private Integer maximumQty;
    private LocalDate deliveryDate;
    private String terms;
}
