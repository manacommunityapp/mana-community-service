package com.manacommunity.api.registration.external.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PublicRegistrationSubmissionResponse(
        String registrationId,
        String status,
        String applicantName,
        RegistrationScope scope,
        Long referenceId,
        String scopeTitle,
        String confirmationCode,
        Instant registeredAt,
        String message
) {}
