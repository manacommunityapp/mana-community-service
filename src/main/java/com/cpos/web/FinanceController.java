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

@Profile("!prod") // mock finance figures; disable in production
@RestController
@RequestMapping("/api/v1/cpos/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMMUNITY_ADMIN')")
public class FinanceController {

    private static boolean isSuperAdmin(UserPrincipal p) {
        if (p == null || p.getAuthorities() == null) return false;
        return p.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/portfolio-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioFinanceSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID tenantId) {
        if (!isSuperAdmin(principal)) {
            if (principal == null || principal.getCommunityId() == null) {
                throw new AccessDeniedException("User is not associated with any community");
            }
            if (!CommunityUuidBridge.matches(principal.getCommunityId(), tenantId)) {
                throw new AccessDeniedException("Cross-community access denied");
            }
        }

        Map<String, Object> fin = new HashMap<>();
        fin.put("tenantId", tenantId);
        fin.put("totalPortfolioValue", 28400000000L); // 2840 Cr
        fin.put("monthlyRentalIncome", 19500000);     // 1.95 Cr
        fin.put("outstandingLoans", 6800000000L);     // 680 Cr
        fin.put("securityDepositsHeld", 52000000);     // 5.2 Cr
        fin.put("collectionRatePct", 96.2);
        return ResponseEntity.ok(fin);
    }
}
