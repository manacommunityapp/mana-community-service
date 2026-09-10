package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SportsTournamentAnnouncementRequest(
        String template,
        @NotBlank String subject,
        @NotBlank String message,
        boolean sendEmail,
        boolean sendPush,
        String customHtml
) {}
