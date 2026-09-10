package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketOffer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferRequest {

    @NotNull(message = "Offered price is required")
    @DecimalMin(value = "0.01", message = "Offered price must be greater than 0")
    private BigDecimal offeredPrice;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
}
