package com.manacommunity.api.email.builder.dto;

import com.manacommunity.api.email.builder.entity.CustomEmailTemplate;
import com.manacommunity.api.email.builder.entity.TemplateStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

public record EmailTemplateResponse(
        Long id,
        Long communityId,
        String name,
        String templateName,
        String subject,
        String html,
        String css,
        Object jsonLayout,
        Object layoutJson,
        String generatedCss,
        TemplateStatus status,
        String category,
        List<String> tags,
        String moduleKey,
        String menuKey,
        String menuLabel,
        String subMenuKey,
        String subMenuLabel,
        String useCase,
        String triggerKey,
        String templateKey,
        String themeName,
        String themeJson,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static EmailTemplateResponse from(CustomEmailTemplate entity) {
        Object layoutObj = parseJson(entity.getLayoutJson());
        return new EmailTemplateResponse(
                entity.getId(),
                entity.getCommunityId(),
                entity.getName(),
                entity.getName(),
                entity.getSubject(),
                entity.getHtml(),
                entity.getCss(),
                layoutObj,
                layoutObj,
                entity.getGeneratedCss(),
                entity.getStatus(),
                entity.getCategory(),
                entity.getTags(),
                entity.getModuleKey(),
                entity.getMenuKey(),
                entity.getMenuLabel(),
                entity.getSubMenuKey(),
                entity.getSubMenuLabel(),
                entity.getUseCase(),
                entity.getTriggerKey(),
                entity.getTemplateKey(),
                entity.getThemeName(),
                entity.getThemeJson(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
