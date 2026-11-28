package com.manacommunity.api.registration.external.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Map;

@Builder
public record PublicRegistrationSubmissionRequest(
        @NotBlank String signedToken,
        @NotBlank String applicantName,
        @NotBlank String phoneNumber,
        @Email String email,
        String teamName,
        String playerRole,
        String preferredJerseyNumber,
        String flatNumber,
        String emergencyContact,
        Map<String, String> additionalFields
) {}
