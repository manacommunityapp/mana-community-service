package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancellationRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
