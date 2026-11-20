package com.manacommunity.api.grouporder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelGroupOrderRequest {
    @NotBlank
    private String reason;
}
