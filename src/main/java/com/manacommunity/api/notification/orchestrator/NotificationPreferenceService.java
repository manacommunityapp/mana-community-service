package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceDto;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceUpdateRequest;

import java.util.List;

public interface NotificationPreferenceService {
    List<NotificationPreferenceDto> getPreferencesForUser(Long userId);
    NotificationPreferenceDto updatePreference(Long userId, NotificationPreferenceUpdateRequest request);
}
