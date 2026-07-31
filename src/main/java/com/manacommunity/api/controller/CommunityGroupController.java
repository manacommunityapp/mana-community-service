package com.manacommunity.api.controller;

import com.manacommunity.api.dto.GroupRequest;
import com.manacommunity.api.dto.GroupResponse;
import com.manacommunity.api.dto.PostResponse;
import com.manacommunity.api.service.CommunityGroupService;
import com.manacommunity.api.service.FeedService;
import com.manacommunity.api.service.EngagementService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class CommunityGroupController {

    private final LoggedInUserService loggedInUserService;
    private final CommunityGroupService groupService;
    private final FeedService feedService;
    private final EngagementService engagementService;

    @GetMapping
    public ResponseEntity<Page<GroupResponse>> getGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupService.getGroups(currentUser, category, search, page, size));
    }

    @GetMapping("/my")
    public ResponseEntity<List<GroupResponse>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupService.getMyGroups(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupService.getGroup(currentUser, id));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GroupRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupService.createGroup(currentUser, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GroupRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupService.updateGroup(currentUser, id, request));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<GroupResponse> joinGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        GroupResponse response = groupService.joinGroup(currentUser, id);
        engagementService.recordActivity(currentUser, "GROUP_JOINED", 5);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        groupService.leaveGroup(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Page<CommunityGroupService.GroupMemberResponse>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupService.getGroupMembers(id, page, size));
    }

    @GetMapping("/{id}/feed")
    public ResponseEntity<Page<PostResponse>> getGroupFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(feedService.getGroupFeed(currentUser, id, page, size));
    }
}
