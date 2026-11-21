package com.manacommunity.api.grouporder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CloseGroupOrderRequest {

    @NotNull @DecimalMin("0.01")
    private BigDecimal finalUnitPrice;

    private String vendorRef;

    private LocalDate deliveryDate;
}
