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

    // No longer validated at registration — email is verified via POST /api/otp/verify at step 3.
    String emailOtpCode;

    /**
     * DPDP Act 2023 s.6 — explicit opt-in consent to the Privacy Notice must be
     * true before the account is created. Validated by AuthServiceImpl.
     * The notice version seen by the user should be passed via consentNoticeVersion.
     */
    @NotNull(message = "You must accept the Privacy Notice to register")
    Boolean consentToPrivacyNotice;

    /** Optional: version string of the privacy notice shown at sign-up. */
    String consentNoticeVersion;

    /**
     * CAPTCHA token from hCaptcha / reCAPTCHA widget on the registration form.
     * Only validated when app.captcha.enabled=true (default false).
     * [LEGAL REVIEW REQUIRED] Ensure chosen CAPTCHA provider's data-processing
     * terms are disclosed in the Privacy Notice.
     */
    String captchaToken;

    /**
     * [LEGAL REVIEW REQUIRED] Aadhaar number is a sensitive government ID under
     * DPDP Act 2023.  If collected, it requires explicit separate consent and
     * purpose limitation.  Review whether this field is strictly necessary;
     * if not, remove it.  Storing Aadhaar may also attract obligations under
     * the Aadhaar Act 2016 (UIDAI regulations).
     */
    String aadharNumber;
}
