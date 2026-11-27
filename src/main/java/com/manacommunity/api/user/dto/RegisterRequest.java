package com.manacommunity.api.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @JsonAlias({"name"})
    String fullName;

    @NotBlank
    String email;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    @JsonAlias({"mobile"})
    String phone;

    
    @JsonAlias({"communityCode"})
    String inviteCode;

    @NotBlank
    String password;

    LocalDate dateOfBirth;

    @NotBlank
    String gender = "OTHER"; // MALE / FEMALE / OTHER (defaults to OTHER for backward compatibility)

    
    @JsonAlias({"flatNumber"})
    String flatNo;

    
    @JsonAlias({"tower"})
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

    /**
     * Sanitizes phone input by stripping formatting and standard country prefix (+91, 0)
     * so that inputs like "+91 98765 43210" normalize to 10 digits before validation.
     */
    public void setPhone(String rawPhone) {
        if (rawPhone != null) {
            String digits = rawPhone.replaceAll("[^0-9]", "");
            if (digits.length() == 12 && digits.startsWith("91")) {
                digits = digits.substring(2);
            } else if (digits.length() == 11 && digits.startsWith("0")) {
                digits = digits.substring(1);
            }
            this.phone = digits;
        } else {
            this.phone = null;
        }
    }
}
