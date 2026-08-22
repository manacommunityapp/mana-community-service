package com.manacommunity.api.controller;

import com.manacommunity.api.notification.dto.UserSmsPreferenceRequest;
import com.manacommunity.api.notification.entity.UserSmsPreference;
import com.manacommunity.api.notification.repository.UserSmsPreferenceRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.model.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
@RequiredArgsConstructor
public class UserSmsPreferenceController {

    private final UserSmsPreferenceRepository preferenceRepository;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<UserSmsPreference>> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(preferenceRepository.findByUserId(user.getId()));
    }

    @PutMapping
    public ResponseEntity<UserSmsPreference> upsertPreference(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserSmsPreferenceRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        UserSmsPreference pref = preferenceRepository
                .findByUserIdAndNotificationType(user.getId(), request.getNotificationType())
                .orElseGet(() -> UserSmsPreference.builder()
                        .userId(user.getId())
                        .notificationType(request.getNotificationType())
                        .build());
        pref.setSmsEnabled(request.isSmsEnabled());
        pref.setWhatsappEnabled(request.isWhatsappEnabled());
        if (request.getPreferredLanguage() != null) {
            pref.setPreferredLanguage(request.getPreferredLanguage());
        }
        return ResponseEntity.ok(preferenceRepository.save(pref));
    }
}
