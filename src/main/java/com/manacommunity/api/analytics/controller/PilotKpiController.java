package com.manacommunity.api.analytics.controller;

import com.manacommunity.api.analytics.dto.PilotKpisResponse;
import com.manacommunity.api.analytics.service.PilotKpiService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class PilotKpiController {

    private final PilotKpiService pilotKpiService;

    @GetMapping("/pilot-kpis")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COMMUNITY_ADMIN') or hasAuthority('MANAGE_COMMUNITY_SETTINGS')")
    public ResponseEntity<PilotKpisResponse> getPilotKpis(
            @RequestParam(required = false) Long communityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean isSuperAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        Long targetCommunityId = communityId;
        if (targetCommunityId == null) {
            targetCommunityId = principal.getCommunityId();
        }

        if (!isSuperAdmin && targetCommunityId != null && !targetCommunityId.equals(principal.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: communityId does not match authenticated user's community");
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(pilotKpiService.getPilotKpis(targetCommunityId, from, to));
    }
}
