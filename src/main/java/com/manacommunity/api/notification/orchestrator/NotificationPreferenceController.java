package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceDto;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationPreferenceDto>> getPreferences(
            @PathVariable Long userId) {
        List<NotificationPreferenceDto> prefs = preferenceService.getPreferencesForUser(userId);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<NotificationPreferenceDto> updatePreference(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        NotificationPreferenceDto updated = preferenceService.updatePreference(userId, request);
        return ResponseEntity.ok(updated);
    }
}
