package com.manacommunity.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendSignupOtpRequest {
    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}
