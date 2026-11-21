package com.manacommunity.api.identity.portability.controller;

import com.manacommunity.api.identity.portability.dto.ImportProfileRequest;
import com.manacommunity.api.identity.portability.dto.ImportResult;
import com.manacommunity.api.identity.portability.dto.InitiateFederationRequest;
import com.manacommunity.api.identity.portability.entity.CommunityFederationLink;
import com.manacommunity.api.identity.portability.entity.ResidentPortabilityToken;
import com.manacommunity.api.identity.portability.entity.ResidentReputationScore;
import com.manacommunity.api.identity.portability.service.ResidentPortabilityService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class PortabilityController {

    private final ResidentPortabilityService portabilityService;
    private final LoggedInUserService loggedInUserService;

    // ── My portability token ─────────────────────────────────────────────────

    @GetMapping("/portability/token")
    public ResponseEntity<ApiResponse<ResidentPortabilityToken>> getOrGenerateToken(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal.getCommunityId();
        ResidentPortabilityToken token = portabilityService.generatePortabilityToken(user.getId(), communityId);
        return ResponseEntity.ok(ApiResponse.ok(token, "Portability token ready — valid 30 days"));
    }

    // ── Reputation score ─────────────────────────────────────────────────────

    @GetMapping("/portability/score")
    public ResponseEntity<ApiResponse<ResidentReputationScore>> getMyScore(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal.getCommunityId();
        ResidentReputationScore score = portabilityService.getMyScore(user.getId(), communityId)
                .orElseGet(() -> portabilityService.computeReputationScore(user.getId(), communityId));
        return ResponseEntity.ok(ApiResponse.ok(score));
    }

    // ── Import profile (target community FM uses this) ───────────────────────

    @PostMapping("/portability/import")
    @PreAuthorize("hasAnyRole('FM', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportResult>> importProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ImportProfileRequest req) {
        Long communityId = principal.getCommunityId();
        ImportResult result = portabilityService.importResidentProfile(communityId, req.getTokenHash());
        return ResponseEntity.ok(ApiResponse.ok(result, result.getMessage()));
    }

    // ── Federation ───────────────────────────────────────────────────────────

    @GetMapping("/federation")
    public ResponseEntity<ApiResponse<List<CommunityFederationLink>>> getFederation(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(ApiResponse.ok(portabilityService.getFederatedCommunities(communityId)));
    }

    @PostMapping("/federation")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommunityFederationLink>> initiateFederation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody InitiateFederationRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = principal.getCommunityId();
        CommunityFederationLink link = portabilityService.initiateFederation(
                communityId, req.getTargetCommunityId(), req.getLinkType(), user.getId());
        return ResponseEntity.ok(ApiResponse.ok(link, "Federation request sent to target community"));
    }

    @PutMapping("/federation/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommunityFederationLink>> approveFederation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        CommunityFederationLink link = portabilityService.approveFederation(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(link, "Federation link activated"));
    }
}
