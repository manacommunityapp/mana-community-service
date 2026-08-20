package com.manacommunity.api.controller;

import com.manacommunity.api.dto.CommunityLeaderRequest;
import com.manacommunity.api.dto.CommunityLeaderResponse;
import com.manacommunity.api.service.CommunityDirectoryService;
import com.manacommunity.api.service.CommunityDirectoryService.DirectoryStats;
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
import java.util.Map;

@RestController
@RequestMapping("/api/community/directory")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommunityDirectoryController {

    private final CommunityDirectoryService directoryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<CommunityLeaderResponse>> getDirectory(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(directoryService.getActiveLeaders(communityId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommunityLeaderResponse>> getAllLeaders(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(directoryService.getAllLeaders(communityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityLeaderResponse> getLeader(@PathVariable Long id) {
        return ResponseEntity.ok(directoryService.getLeaderById(id));
    }

    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<CommunityLeaderResponse>>> getGrouped(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(directoryService.getGroupedByCommittee(communityId));
    }

    @GetMapping("/stats")
    public ResponseEntity<DirectoryStats> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(directoryService.getStats(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> addLeader(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityLeaderRequest req) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directoryService.addLeader(communityId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> updateLeader(
            @PathVariable Long id,
            @Valid @RequestBody CommunityLeaderRequest req) {
        return ResponseEntity.ok(directoryService.updateLeader(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> removeLeader(@PathVariable Long id) {
        directoryService.removeLeader(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<CommunityLeaderResponse> restoreLeader(@PathVariable Long id) {
        return ResponseEntity.ok(directoryService.restoreLeader(id));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> reorder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody List<Long> orderedIds) {
        Long communityId = resolveCommunityId(principal);
        directoryService.reorder(communityId, orderedIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/designations")
    public ResponseEntity<List<com.manacommunity.api.dto.CommunityDesignationResponse>> getDesignations(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(directoryService.getDesignations(communityId));
    }

    @PostMapping("/designations")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<com.manacommunity.api.dto.CommunityDesignationResponse> addDesignation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody com.manacommunity.api.dto.CommunityDesignationRequest req) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directoryService.addDesignation(communityId, req));
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity().getId();
    }
}
