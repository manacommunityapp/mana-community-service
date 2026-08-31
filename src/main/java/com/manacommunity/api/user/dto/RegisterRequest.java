package com.manacommunity.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Self-registration payload.
 * All residential fields (block, flatNo) are mandatory for apartment communities.
 * Validation against the community's actual block/flat config is enforced by
 * CommunityBlockConfigService.validateBlockAndFlat in AuthServiceImpl.
 */
@Data
public class RegisterRequest {
    @NotBlank
    String fullName;
    @NotBlank
    String email;
    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    String phone;
    String aadharNumber;
    @NotBlank
    String inviteCode;
    @NotBlank
    String password;
    @NotNull
    @Past
    LocalDate dateOfBirth;
    @NotBlank
    String gender; // MALE / FEMALE / OTHER
    @NotBlank
    String flatNo;
    @NotBlank
    String block;  // A, B, C, D — mandatory; validated against community_block_config
    String userType;        // OWNER / TENANT / Owner / Tenant
    String occupancyStatus; // Owner / Tenant / Staff
    String residentType;    // Resident / Non-Resident / Guest

    @NotBlank(message = "Email verification code is required")
    @Size(min = 4, max = 9, message = "Invalid verification code")
    String emailOtpCode;
}
