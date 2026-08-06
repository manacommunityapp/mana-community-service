package com.manacommunity.api.email.builder.service;

import com.manacommunity.api.email.builder.dto.EmailThemeRequest;
import com.manacommunity.api.email.builder.entity.EmailThemeConfig;
import com.manacommunity.api.email.builder.repository.EmailThemeConfigRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailThemeBuilderService {

    private final EmailThemeConfigRepository repository;

    public List<EmailThemeConfig> listByCommunity(Long communityId) {
        return repository.findByCommunityIdOrderByUpdatedAtDesc(communityId);
    }

    @Transactional
    public EmailThemeConfig save(EmailThemeRequest request) {
        EmailThemeConfig entity;

        if (request.id() != null) {
            entity = repository.findById(request.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Email theme", request.id()));
        } else {
            entity = EmailThemeConfig.builder().build();
        }

        entity.setCommunityId(request.communityId());
        entity.setName(request.name());
        entity.setThemeJson(request.themeJson());

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        entity.setIsDefault(makeDefault);

        if (makeDefault) {
            clearDefaultFlag(request.communityId(), entity.getId());
        }

        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Email theme", id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void applyThemeToTemplate(Long themeId, Long templateId,
                                     EmailTemplateBuilderService templateService) {
        EmailThemeConfig theme = repository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Email theme", themeId));
        // The actual theme application (CSS generation) happens on the frontend.
        // This endpoint is a convenience hook for future server-side theme rendering.
    }

    private void clearDefaultFlag(Long communityId, Long excludeId) {
        repository.findByCommunityIdAndIsDefaultTrue(communityId)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    repository.save(existing);
                });
    }
}
