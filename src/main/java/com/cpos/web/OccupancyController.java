package com.cpos.web;

import com.cpos.domain.common.CommunityUuidBridge;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("!prod") // static placeholder figures; never serve in production
@RestController
@RequestMapping("/api/v1/cpos/occupancy")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMMUNITY_ADMIN')")
public class OccupancyController {

    private static boolean isSuperAdmin(UserPrincipal p) {
        if (p == null || p.getAuthorities() == null) return false;
        return p.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/community/{communityId}/summary")
    public ResponseEntity<Map<String, Object>> getOccupancySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID communityId) {
        if (!isSuperAdmin(principal)) {
            if (principal == null || principal.getCommunityId() == null) {
                throw new AccessDeniedException("User is not associated with any community");
            }
            if (!CommunityUuidBridge.matches(principal.getCommunityId(), communityId)) {
                throw new AccessDeniedException("Cross-community access denied");
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("communityId", communityId);
        summary.put("totalUnits", 2847);
        summary.put("ownerOccupied", 1240);
        summary.put("tenantOccupied", 1180);
        summary.put("vacant", 322);
        summary.put("reserved", 105);
        summary.put("occupancyRatePct", 84.9);
        return ResponseEntity.ok(summary);
    }
}
