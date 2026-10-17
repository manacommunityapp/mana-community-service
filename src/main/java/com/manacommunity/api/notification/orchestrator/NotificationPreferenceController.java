package com.manacommunity.api.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceDto;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.NotificationPreferenceUpdateRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationPreferenceDto>> getPreferences(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertUserAccess(userId, principal);
        List<NotificationPreferenceDto> prefs = preferenceService.getPreferencesForUser(userId);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<NotificationPreferenceDto> updatePreference(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationPreferenceUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertUserAccess(userId, principal);
        NotificationPreferenceDto updated = preferenceService.updatePreference(userId, request);
        return ResponseEntity.ok(updated);
    }

    private void assertUserAccess(Long userId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        AppUser user = loggedInUserService.resolve(principal);
        if (user.hasRole("SUPER_ADMIN") || user.hasRole("ADMIN") || user.hasRole("COMMUNITY_ADMIN")) {
            return;
        }
        if (!userId.equals(user.getId())) {
            throw new AccessDeniedException("You may only access your own notification preferences.");
        }
    }
}
