package com.manacommunity.api.controller;

import com.manacommunity.api.dto.push.RegisterPushTokenRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.PushToken;
import com.manacommunity.api.repository.PushTokenRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Manages Expo push tokens for the mobile app.
 *
 * Called by the mobile hook (usePushNotifications.ts):
 *   POST   /api/users/push-token  — on first launch / after permission granted
 *   DELETE /api/users/push-token  — on explicit logout
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenRepository tokenRepo;
    private final LoggedInUserService  loggedInUserService;

    /**
     * POST /api/users/push-token
     * Body: { "token": "ExponentPushToken[...]", "platform": "ios" | "android" }
     *
     * Upserts the token: creates if new, re-activates if it was previously
     * deactivated (e.g. user reinstalled the app).
     */
    @PostMapping("/push-token")
    @Transactional
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterPushTokenRequest request) {

        AppUser user = loggedInUserService.resolve(principal);

        // Validate Expo token format
        String token = request.getToken().trim();
        if (!token.startsWith("ExponentPushToken[")) {
            log.warn("Invalid push token format from user {}: {}", user.getId(),
                     token.length() > 20 ? token.substring(0, 20) + "…" : token);
            return ResponseEntity.badRequest().build();
        }

        // Upsert: find existing or create new
        PushToken pushToken = tokenRepo.findByToken(token)
                .orElse(PushToken.builder()
                        .token(token)
                        .user(user)
                        .platform(request.getPlatform())
                        .build());

        // Re-activate if it was deactivated (reinstall scenario)
        pushToken.setActive(true);
        pushToken.setUser(user);             // re-associate if token transferred (device swap)
        pushToken.setPlatform(request.getPlatform());

        tokenRepo.save(pushToken);
        log.info("Push token registered: userId={} platform={}", user.getId(), request.getPlatform());

        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/users/push-token
     * Body: { "token": "ExponentPushToken[...]" }
     *
     * Deactivates the specific device token on logout.
     * Does NOT delete the row — keeps it for audit trail and
     * efficient cleanup via the scheduled purge job.
     */
    @DeleteMapping("/push-token")
    @Transactional
    public ResponseEntity<Void> deregisterToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AppUser user = loggedInUserService.resolve(principal);
        tokenRepo.deactivateByToken(token.trim());
        log.info("Push token deactivated: userId={}", user.getId());

        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/users/push-token/all
     * Deactivates ALL tokens for the current user (logout from all devices).
     */
    @DeleteMapping("/push-token/all")
    @Transactional
    public ResponseEntity<Void> deregisterAllTokens(
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser user = loggedInUserService.resolve(principal);
        tokenRepo.deactivateAllByUserId(user.getId());
        log.info("All push tokens deactivated: userId={}", user.getId());

        return ResponseEntity.ok().build();
    }
}
