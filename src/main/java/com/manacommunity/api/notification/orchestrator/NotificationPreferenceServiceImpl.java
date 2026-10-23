package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceDto;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceUpdateRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final AppUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferencesForUser(Long userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationPreferenceDto updatePreference(Long userId, NotificationPreferenceUpdateRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        NotificationPreference pref = preferenceRepository.findByUserIdAndCategoryAndChannel(
                userId, request.getCategory(), request.getChannel())
                .orElseGet(() -> NotificationPreference.builder()
                        .user(user)
                        .category(request.getCategory())
                        .channel(request.getChannel())
                        .build());

        if (request.getIsEnabled() != null) {
            pref.setIsEnabled(request.getIsEnabled());
        }
        if (request.getQuietHoursEnabled() != null) {
            pref.setQuietHoursEnabled(request.getQuietHoursEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            pref.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            pref.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        if (request.getTimezone() != null) {
            pref.setTimezone(request.getTimezone());
        }

        NotificationPreference saved = preferenceRepository.save(pref);
        return mapToDto(saved);
    }

    private NotificationPreferenceDto mapToDto(NotificationPreference pref) {
        return NotificationPreferenceDto.builder()
                .id(pref.getId())
                .category(pref.getCategory())
                .channel(pref.getChannel())
                .isEnabled(Boolean.TRUE.equals(pref.getIsEnabled()))
                .quietHoursEnabled(Boolean.TRUE.equals(pref.getQuietHoursEnabled()))
                .quietHoursStart(pref.getQuietHoursStart())
                .quietHoursEnd(pref.getQuietHoursEnd())
                .timezone(pref.getTimezone())
                .build();
    }
}
