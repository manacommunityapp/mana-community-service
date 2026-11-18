package com.manacommunity.api.payments.analytics;

import com.manacommunity.api.payments.analytics.dto.PaymentAnalyticsResponse;
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
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAnalyticsController {

    private final PaymentAnalyticsService analyticsService;

    /**
     * Returns payment analytics for a community and date range.
     * Community isolation: SUPER_ADMIN may query any communityId; all other roles
     * are restricted to their own community.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('MANAGE_COMMUNITY_SETTINGS')")
    public ResponseEntity<PaymentAnalyticsResponse> getAnalytics(
            @RequestParam Long communityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Non-SUPER_ADMIN users may only view their own community's data
        boolean isSuperAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (!isSuperAdmin && !communityId.equals(principal.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: communityId does not match authenticated user's community");
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getAnalytics(communityId, from, to));
    }
}
