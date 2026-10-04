package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PickupVerificationRequest {
    @NotBlank
    private String qrToken;
}
