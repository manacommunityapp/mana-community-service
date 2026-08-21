package com.manacommunity.api.controller;

import com.manacommunity.api.dto.CommunityWhoToCallHistoryResponse;
import com.manacommunity.api.dto.CommunityWhoToCallRequest;
import com.manacommunity.api.dto.CommunityWhoToCallResponse;
import com.manacommunity.api.service.CommunityWhoToCallService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community/who-to-call")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommunityWhoToCallController {

    private final CommunityWhoToCallService whoToCallService;
    private final LoggedInUserService loggedInUserService;

    /** Public active directory for community residents */
    @GetMapping
    public ResponseEntity<List<CommunityWhoToCallResponse>> getActive(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(whoToCallService.getActive(communityId));
    }

    /** Admin view of all contacts including inactive */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommunityWhoToCallResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(whoToCallService.getAll(communityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityWhoToCallResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(whoToCallService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityWhoToCallResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityWhoToCallRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new com.manacommunity.api.exception.InvalidInputException("User is not associated with any community.");
        }
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(whoToCallService.create(communityId, user.getId(), user.getFullName(), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityWhoToCallResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityWhoToCallRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(whoToCallService.update(id, user.getId(), user.getFullName(), req));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        whoToCallService.toggleStatus(id, user.getId(), user.getFullName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        whoToCallService.delete(id, user.getId(), user.getFullName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityWhoToCallResponse> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(whoToCallService.restore(id, user.getId(), user.getFullName()));
    }

    /** Community-wide Who to Call change history audit log */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommunityWhoToCallHistoryResponse>> getCommunityHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(whoToCallService.getAllCommunityHistory(communityId));
    }

    /** Specific contact history */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommunityWhoToCallHistoryResponse>> getContactHistory(@PathVariable Long id) {
        return ResponseEntity.ok(whoToCallService.getHistory(id));
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new com.manacommunity.api.exception.InvalidInputException("User is not associated with any community.");
        }
        return user.getCommunity().getId();
    }
}
