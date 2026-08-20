package com.manacommunity.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "OTP verification code is required")
    private String otpCode;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
