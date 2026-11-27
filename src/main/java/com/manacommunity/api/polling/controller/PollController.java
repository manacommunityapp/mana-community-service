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

    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasAuthority('View Polls')")
    public ResponseEntity<com.manacommunity.api.governance.service.VotingEligibilityService.EligibilityResult> checkEligibility(
            @PathVariable Long id,
            @RequestParam Long flatId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(pollService.checkEligibility(id, flatId, user.getId()));
    }

    @PostMapping("/{id}/vote")
    @PreAuthorize("hasAuthority('Vote Poll')")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        @SuppressWarnings("unchecked")
        List<Long> optionIds = ((List<?>) body.get("optionIds")).stream()
                .map(o -> Long.valueOf(o.toString()))
                .toList();
        Object flatRaw = body.get("flatId");
        if (flatRaw != null) {
            return ResponseEntity.ok(pollService.voteForFlat(id, Long.valueOf(flatRaw.toString()), optionIds, user));
        }
        return ResponseEntity.ok(pollService.vote(id, optionIds, user));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('Create Poll')")
    public ResponseEntity<PollResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(pollService.closePoll(id, user.getId()));
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
