package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.entity.MarketDataDeletionRequest;
import com.manacommunity.api.marketplace.service.MarketPrivacyService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/marketplace/privacy", "/api/v1/marketplace/privacy"})
@RequiredArgsConstructor
public class MarketPrivacyController {

    private final MarketPrivacyService privacyService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/deletion-requests")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketDataDeletionRequest>> getMyDeletionRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(privacyService.getUserDeletionRequests(user.getId()));
    }

    @PostMapping("/deletion-request")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketDataDeletionRequest> requestDeletion(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(privacyService.requestDataDeletion(user, reason));
    }

    @PutMapping("/deletion-requests/{id}/status")
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<MarketDataDeletionRequest> processDeletion(
            @PathVariable Long id,
            @RequestParam MarketDataDeletionRequest.DeletionStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser admin = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(privacyService.processDeletionRequest(id, status, admin));
    }
}
