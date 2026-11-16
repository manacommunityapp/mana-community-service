package com.manacommunity.api.payments.analytics;

import com.manacommunity.api.payments.analytics.dto.PaymentAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAnalyticsController {

    private final PaymentAnalyticsService analyticsService;

    /**
     * Returns payment analytics for a community and date range.
     * Community-level data isolation: communityId is a required parameter.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('MANAGE_COMMUNITY_SETTINGS')")
    public ResponseEntity<PaymentAnalyticsResponse> getAnalytics(
            @RequestParam Long communityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getAnalytics(communityId, from, to));
    }
}
