package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyCircleDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyCircleService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kitty-circles")
@RequiredArgsConstructor
public class KittyPartyCircleController {

    private final KittyPartyCircleService circleService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<CircleResponse> createCircle(
            @Valid @RequestBody CreateCircleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        CircleResponse response = circleService.createCircle(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<CircleResponse>> getMyCircles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(circleService.getCirclesForUser(user.getId(), pageable));
    }

    @GetMapping("/discover")
    public ResponseEntity<Page<CircleResponse>> discoverCircles(
            @RequestParam(required = false) String categoryCode,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(circleService.discoverCircles(communityId, categoryCode, pageable));
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<CircleResponse> getCircleDetails(
            @PathVariable Long circleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(circleService.getCircleDetails(circleId, user.getId()));
    }

    @GetMapping("/{circleId}/members")
    public ResponseEntity<List<MemberResponse>> getCircleMembers(@PathVariable Long circleId) {
        return ResponseEntity.ok(circleService.getCircleMembers(circleId));
    }

    @PostMapping("/{circleId}/join")
    public ResponseEntity<Map<String, Object>> requestToJoin(
            @PathVariable Long circleId,
            @RequestBody(required = false) JoinRequestPayload payload,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        circleService.requestToJoin(circleId, user, payload);
        return ResponseEntity.ok(Map.of("success", true, "message", "Join request submitted"));
    }

    @PostMapping("/{circleId}/join-requests/{requestId}/approve")
    public ResponseEntity<Map<String, Object>> approveJoinRequest(
            @PathVariable Long circleId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        circleService.approveJoinRequest(circleId, requestId, user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Join request approved"));
    }

    @PostMapping("/{circleId}/invitations")
    public ResponseEntity<Map<String, Object>> inviteMember(
            @PathVariable Long circleId,
            @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        String token = circleService.inviteMember(circleId, user, request);
        return ResponseEntity.ok(Map.of("success", true, "inviteToken", token));
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @RequestParam String token,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        circleService.acceptInvitation(token, user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Invitation accepted"));
    }
}
