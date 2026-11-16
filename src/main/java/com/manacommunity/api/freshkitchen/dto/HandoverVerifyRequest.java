package com.manacommunity.api.freshkitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverVerifyRequest {
    private String qrToken;
    private String orderNumber;
    private String handoverOtp;
    private String runnerNotes;
}
