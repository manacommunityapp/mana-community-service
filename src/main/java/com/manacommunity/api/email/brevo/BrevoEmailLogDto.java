package com.manacommunity.api.email.brevo;

import lombok.Builder;
import java.util.List;

@Builder
public record BrevoEmailLogDto(
        String email,
        String subject,
        String messageId,
        String uuid,
        String date,
        String status,
        List<BrevoEventDetailDto> events,
        String templateId,
        String from
) {
    @Builder
    public record BrevoEventDetailDto(
            String name,
            String time,
            String reason
    ) {}
}