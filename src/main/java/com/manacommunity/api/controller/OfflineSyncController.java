package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SyncPullRequest;
import com.manacommunity.api.dto.SyncPullResponse;
import com.manacommunity.api.dto.SyncPushRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Disabled in favor of com.manacommunity.api.sync.OfflineSyncController
// @RestController
// @RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class OfflineSyncController {

    private static final Logger log = LoggerFactory.getLogger(OfflineSyncController.class);

    private final LoggedInUserService loggedInUserService;

    /**
     * Receives queued offline mutations and replays/acknowledges them.
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> pushSync(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SyncPushRequest request) {
        AppUser user = loggedInUserService.resolve(principal);

        log.info("Sync push from user {} (community {}): {} actions",
                user.getId(), user.getCommunity().getId(), request.actions().size());

        int processed = 0;
        int failed = 0;

        for (SyncPushRequest.SyncAction action : request.actions()) {
            try {
                log.info("Replaying sync action: id={}, service={}, method={}, timestamp={}",
                        action.id(), action.service(), action.method(), action.timestamp());
                // In production, dispatch to the appropriate service based on action.service()
                // and call action.method() with action.args()
                processed++;
            } catch (Exception e) {
                log.error("Failed to replay sync action {}: {}", action.id(), e.getMessage());
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalReceived", request.actions().size());
        result.put("processed", processed);
        result.put("failed", failed);
        result.put("syncTimestamp", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }

    /**
     * Returns changes since a given timestamp for the user's community.
     */
    @PostMapping("/pull")
    public ResponseEntity<SyncPullResponse> pullSync(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SyncPullRequest request) {
        AppUser user = loggedInUserService.resolve(principal);

        log.info("Sync pull from user {} (community {}): since {}",
                user.getId(), user.getCommunity().getId(), request.since());

        // In production, query each entity table for records created/updated after request.since()
        // scoped to the user's community
        Map<String, List<Map<String, Object>>> updates = new HashMap<>();
        // Placeholder: return empty updates — real implementation would query relevant tables

        SyncPullResponse response = new SyncPullResponse(LocalDateTime.now(), updates);
        return ResponseEntity.ok(response);
    }
}
