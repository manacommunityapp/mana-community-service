package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketHandoverPass;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketHandoverPassDto {

    private Long id;
    private Long orderId;
    private String passQrCode;
    private String otpPin; // Plain PIN returned only upon initial generation to buyer
    private MarketHandoverPass.PassStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private String verifiedByGuardName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        @NotBlank(message = "Pass code or QR is required")
        private String passQrCode;

        @NotBlank(message = "4-digit OTP PIN is required")
        private String pin;
    }
}
