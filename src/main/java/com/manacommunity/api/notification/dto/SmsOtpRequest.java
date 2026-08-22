package com.manacommunity.api.notification.dto;

import com.manacommunity.api.notification.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsOtpRequest {
    @NotBlank
    private String phoneNumber;
    @NotNull
    private OtpPurpose purpose;
}
