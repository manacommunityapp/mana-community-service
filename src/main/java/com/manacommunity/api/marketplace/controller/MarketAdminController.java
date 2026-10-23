package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketAdminMetricsDto;
import com.manacommunity.api.marketplace.dto.MarketAuditLogDto;
import com.manacommunity.api.marketplace.dto.MarketReportRequest;
import com.manacommunity.api.marketplace.dto.MarketReportResponse;
import com.manacommunity.api.marketplace.dto.MarketSellerAnalyticsDto;
import com.manacommunity.api.marketplace.service.MarketAdminService;
import com.manacommunity.api.marketplace.service.MarketModerationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/marketplace/admin", "/api/v1/marketplace/admin"})
@RequiredArgsConstructor
public class MarketAdminController {

    private final MarketAdminService adminService;
    private final MarketModerationService moderationService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<MarketAdminMetricsDto> getMetrics(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(MarketAdminMetricsDto.builder().build());
        return ResponseEntity.ok(adminService.getAdminMetrics(communityId));
    }

    @GetMapping("/seller-analytics")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketSellerAnalyticsDto> getSellerAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(adminService.getSellerAnalytics(user.getId()));
    }

    @GetMapping("/moderation/reports")
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<Page<MarketReportResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(moderationService.getPendingReports(communityId, pageable));
    }

    @PostMapping("/moderation/report")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketReportResponse> reportListing(
            @Valid @RequestBody MarketReportRequest.Create req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderationService.reportListing(req, user, user.getCommunity()));
    }

    @PutMapping("/moderation/reports/{id}")
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<MarketReportResponse> moderateReport(
            @PathVariable Long id,
            @Valid @RequestBody MarketReportRequest.Moderate req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(moderationService.moderateReport(id, req, user));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<Page<MarketAuditLogDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(adminService.getAuditLogs(communityId, pageable));
    }
}
