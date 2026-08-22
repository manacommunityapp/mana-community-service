package com.manacommunity.api.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @JsonAlias({"currentPassword", "oldPassword"})
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @JsonAlias({"newPassword", "password"})
    private String newPassword;

    @JsonAlias({"confirmPassword"})
    private String confirmPassword;
}