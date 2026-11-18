package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiateManaPayRequest {
    @NotNull(message = "orderId is required")
    private Long orderId;
}
