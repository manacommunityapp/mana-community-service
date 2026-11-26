package com.manacommunity.api.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressVerifyRequest {
    @NotBlank(message = "entryCode or barcode is required")
    private String entryCode;
    private Long communityId;
    private String gateId;
}
