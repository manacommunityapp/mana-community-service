package com.manacommunity.api.dto.email;

import com.manacommunity.api.model.EmailTemplateStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;

public final class EmailTemplateManagementDtos {

    private EmailTemplateManagementDtos() {
    }

    public record BrandingRequest(
            String logoUrl,
            String primaryColor,
            String secondaryColor,
            String fontFamily,
            String buttonStyle,
            String bannerUrl,
            String facebookUrl,
            String instagramUrl,
            String websiteUrl,
            String supportEmail,
            String supportPhone
    ) {
    }

    public record BrandingResponse(
            Long id,
            Long communityId,
            String communityName,
            String logoUrl,
            String primaryColor,
            String secondaryColor,
            String fontFamily,
            String buttonStyle,
            String bannerUrl,
            String facebookUrl,
            String instagramUrl,
            String websiteUrl,
            String supportEmail,
            String supportPhone
    ) {
    }

    public record HeaderFooterRequest(
            @NotBlank String name,
            @NotBlank String htmlContent,
            String layoutJson,
            Boolean active,
            Long createdBy
    ) {
    }

    public record HeaderFooterResponse(
            Long id,
            Long communityId,
            String name,
            String htmlContent,
            String layoutJson,
            boolean active,
            Integer version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record TemplateRequest(
            @NotBlank String templateCode,
            @NotBlank String templateName,
            String category,
            @NotBlank String subject,
            @NotBlank String htmlContent,
            String layoutJson,
            Long headerId,
            Long footerId,
            EmailTemplateStatus status,
            Long createdBy,
            String notes
    ) {
    }

    public record TemplateSummaryResponse(
            Long id,
            Long communityId,
            String templateCode,
            String templateName,
            String category,
            String subject,
            EmailTemplateStatus status,
            Integer version,
            LocalDateTime updatedAt
    ) {
    }

    public record TemplateDetailResponse(
            Long id,
            Long communityId,
            String templateCode,
            String templateName,
            String category,
            String subject,
            String htmlContent,
            String layoutJson,
            Long headerId,
            Long footerId,
            EmailTemplateStatus status,
            Integer version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record DynamicFieldResponse(
            String category,
            String fieldKey,
            String displayName,
            String sampleValue,
            String description
    ) {
    }

    public record PreviewRequest(Map<String, Object> data) {
    }

    public record PreviewResponse(String subject, String html) {
    }
}
