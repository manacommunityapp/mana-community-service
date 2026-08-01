package com.manacommunity.api.dto.email;

import com.manacommunity.api.model.EmailTemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class EmailBuilderTemplateDtos {

    private EmailBuilderTemplateDtos() {
    }

    public record TemplateRequest(
            Long id,
            @NotNull Long communityId,
            @NotBlank String templateName,
            @NotBlank String subject,
            @NotBlank String html,
            String css,
            Object jsonLayout,
            EmailTemplateStatus status
    ) {
    }

    public record TemplateResponse(
            Long id,
            Long communityId,
            String name,
            String subject,
            String html,
            String css,
            Object jsonLayout,
            EmailTemplateStatus status,
            Integer version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AssetResponse(String url, String key) {
    }
}
