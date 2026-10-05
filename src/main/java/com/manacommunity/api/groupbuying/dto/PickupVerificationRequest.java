package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupVerificationRequest {
    @NotBlank
    private String qrToken;
}

