package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceDto;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceUpdateRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/me/notification-preferences", "/v1/me/notification-preferences", "/api/api/v1/me/notification-preferences", "/api/me/notification-preferences", "/me/notification-preferences"})
@RequiredArgsConstructor
public class NotificationPreferenceMeController {

    private final NotificationPreferenceService preferenceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceDto>> getMyPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(preferenceService.getPreferencesForUser(user.getId()));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDto> updateMyPreference(
            @RequestBody NotificationPreferenceUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(preferenceService.updatePreference(user.getId(), request));
    }
}
