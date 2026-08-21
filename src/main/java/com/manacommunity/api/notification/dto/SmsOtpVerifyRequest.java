package com.manacommunity.api.notification.dto;

import com.manacommunity.api.notification.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsOtpVerifyRequest {
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String otp;
    @NotNull
    private OtpPurpose purpose;
}
