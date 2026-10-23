package com.manacommunity.api.controller;

import com.manacommunity.api.service.UserHubUsageService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hub-usage")
@RequiredArgsConstructor
public class UserHubUsageController {

    private final UserHubUsageService hubUsageService;

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackClick(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        Long effectiveUserId = principal.getId();
        if (request.containsKey("userId") && request.get("userId") != null && !request.get("userId").isBlank()) {
            Long requestedUserId = Long.parseLong(request.get("userId"));
            boolean isAdmin = principal.getAuthorities().stream().anyMatch(a ->
                    "ROLE_SUPER_ADMIN".equals(a.getAuthority()) ||
                    "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin && !effectiveUserId.equals(requestedUserId)) {
                throw new AccessDeniedException("Cannot track usage on behalf of another user.");
            }
            effectiveUserId = requestedUserId;
        }

        String hubId = request.get("hubId");
        String hubLabel = request.get("hubLabel");

        hubUsageService.trackClick(effectiveUserId, hubId, hubLabel);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/top/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTopHubs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertUserAccess(userId, principal);
        return ResponseEntity.ok(hubUsageService.getTopHubs(userId, limit));
    }

    private void assertUserAccess(Long userId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a ->
                "ROLE_SUPER_ADMIN".equals(a.getAuthority()) ||
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_COMMUNITY_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !userId.equals(principal.getId())) {
            throw new AccessDeniedException("You may only access your own hub usage metrics.");
        }
    }
}
