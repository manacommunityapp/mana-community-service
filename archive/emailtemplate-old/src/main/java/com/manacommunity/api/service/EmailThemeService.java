package com.manacommunity.api.service;

import com.manacommunity.api.dto.email.EmailThemeDto;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.EmailBuilderTemplate;
import com.manacommunity.api.model.EmailTheme;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.EmailBuilderTemplateRepository;
import com.manacommunity.api.repository.EmailThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailThemeService {

    private final EmailThemeRepository themeRepository;
    private final CommunityRepository communityRepository;
    private final EmailBuilderTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<EmailThemeDto> listByCommunity(Long communityId) {
        return themeRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(EmailThemeDto::from).toList();
    }

    @Transactional
    public EmailThemeDto save(EmailThemeDto dto) {
        Community community = communityRepository.findById(dto.getCommunityId())
                .orElseThrow(() -> new IllegalArgumentException("Community not found: " + dto.getCommunityId()));

        EmailTheme theme;
        if (dto.getId() != null) {
            theme = themeRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + dto.getId()));
            theme.setName(dto.getName());
            theme.setThemeJson(dto.getThemeJson());
            theme.setDefault(dto.isDefault());
        } else {
            theme = EmailTheme.builder()
                    .community(community)
                    .name(dto.getName())
                    .themeJson(dto.getThemeJson())
                    .isDefault(dto.isDefault())
                    .build();
        }

        if (dto.isDefault()) {
            // Clear previous default for the community
            themeRepository.findByCommunityIdAndIsDefaultTrue(community.getId())
                    .filter(existing -> !existing.getId().equals(theme.getId()))
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        themeRepository.save(existing);
                    });
        }

        return EmailThemeDto.from(themeRepository.save(theme));
    }

    @Transactional
    public void delete(Long id) {
        themeRepository.deleteById(id);
    }

    @Transactional
    public void applyThemeToTemplate(Long themeId, Long templateId) {
        EmailTheme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));
        EmailBuilderTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        template.setThemeJson(theme.getThemeJson());
        templateRepository.save(template);
        log.info("Applied theme '{}' to template '{}'.", theme.getName(), template.getName());
    }
}
