package com.manacommunity.api.email.builder.dto;

import com.manacommunity.api.email.builder.entity.TemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EmailTemplateRequest(
        Long id,
        @NotNull Long communityId,
        @NotBlank String templateName,
        @NotBlank String subject,
        String html,
        String css,
        Object layoutJson,
        Object jsonLayout,
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
        String generatedCss
) {}
