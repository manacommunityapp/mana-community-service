package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrationRequest {
    @NotNull
    Long eventId;
    @NotNull
    Long categoryId;
    String matchType;
    Long formatId;
    String playerName;
    String email;
    String relation;
    String flatNumber;
    Integer age;
    String role;
    Long partnerUserId;
    Long partnerFamilyMemberId;
    Long familyMemberId;
    String cricHeroesUrl;

    /**
     * Google reCAPTCHA token from the public registration form. Only verified
     * when {@code app.security.registration.recaptcha.enabled=true}; ignored
     * (may be null) for authenticated/admin add flows.
     */
    String recaptchaToken;
}
