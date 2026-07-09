package com.manacommunity.api.polling.controller;

import com.manacommunity.api.polling.dto.PollRequest;
import com.manacommunity.api.polling.dto.PollResponse;
import com.manacommunity.api.polling.service.PollService;
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
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Polls')")
    public ResponseEntity<List<PollResponse>> getActivePolls(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(pollService.getActivePolls(communityId, user.getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('View Polls')")
    public ResponseEntity<List<PollResponse>> getAllPolls(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(pollService.getAllPolls(communityId, user.getId()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Polls')")
    public ResponseEntity<List<PollResponse>> getMyPolls(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(pollService.getMyPolls(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Polls')")
    public ResponseEntity<PollResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(pollService.getById(id, user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Poll')")
    public ResponseEntity<PollResponse> create(
            @Valid @RequestBody PollRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pollService.create(req, user, user.getCommunity()));
    }

    @PostMapping("/{id}/vote")
    @PreAuthorize("hasAuthority('Vote Poll')")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(pollService.vote(id, body.get("optionIds"), user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Poll')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        pollService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
