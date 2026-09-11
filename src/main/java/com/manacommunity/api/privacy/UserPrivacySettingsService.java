package com.manacommunity.api.privacy;

import com.manacommunity.api.privacy.dto.UserPrivacySettingsDto;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPrivacySettingsService {

    private final UserPrivacySettingsRepository repo;
    private final PrivacyAuditService privacyAuditService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserPrivacySettingsDto getSettings(Long userId) {
        UserPrivacySettings settings = repo.findByUserId(userId)
                .orElseGet(() -> defaultSettings(userId));
        return toDto(settings);
    }

    @Transactional
    public UserPrivacySettingsDto updateSettings(Long userId, UserPrivacySettingsDto dto) {
        UserPrivacySettings settings = repo.findByUserId(userId)
                .orElseGet(() -> UserPrivacySettings.builder().userId(userId).build());

        if (dto.getShowPhoneToNeighbours() != null) {
            settings.setShowPhoneToNeighbours(dto.getShowPhoneToNeighbours());
        }
        if (dto.getShowEmailToNeighbours() != null) {
            settings.setShowEmailToNeighbours(dto.getShowEmailToNeighbours());
        }
        if (dto.getShowFlatInDirectory() != null) {
            settings.setShowFlatInDirectory(dto.getShowFlatInDirectory());
        }
        if (dto.getShowFamilyMembers() != null) {
            settings.setShowFamilyMembers(dto.getShowFamilyMembers());
        }
        if (dto.getAllowMarketplaceContact() != null) {
            settings.setAllowMarketplaceContact(dto.getAllowMarketplaceContact());
        }
        if (dto.getAllowEventTagging() != null) {
            settings.setAllowEventTagging(dto.getAllowEventTagging());
        }
        if (dto.getActivityVisibility() != null && !dto.getActivityVisibility().isBlank()) {
            settings.setActivityVisibility(dto.getActivityVisibility());
        }

        UserPrivacySettings saved = repo.save(settings);

        privacyAuditService.record(
                AuditAction.UPDATE_PERSONAL_DATA.name(),
                "USER_PRIVACY_SETTINGS",
                String.valueOf(userId)
        );
        auditService.record(
                AuditAction.CONFIG_UPDATED,
                AuditModule.PRIVACY,
                "UserPrivacySettings",
                String.valueOf(userId)
        );

        return toDto(saved);
    }

    private UserPrivacySettings defaultSettings(Long userId) {
        return UserPrivacySettings.builder()
                .userId(userId)
                .showPhoneToNeighbours(false)
                .showEmailToNeighbours(false)
                .showFlatInDirectory(true)
                .showFamilyMembers(false)
                .allowMarketplaceContact(true)
                .allowEventTagging(true)
                .activityVisibility("COMMUNITY")
                .build();
    }

    private UserPrivacySettingsDto toDto(UserPrivacySettings entity) {
        return UserPrivacySettingsDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .showPhoneToNeighbours(entity.getShowPhoneToNeighbours())
                .showEmailToNeighbours(entity.getShowEmailToNeighbours())
                .showFlatInDirectory(entity.getShowFlatInDirectory())
                .showFamilyMembers(entity.getShowFamilyMembers())
                .allowMarketplaceContact(entity.getAllowMarketplaceContact())
                .allowEventTagging(entity.getAllowEventTagging())
                .activityVisibility(entity.getActivityVisibility())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
