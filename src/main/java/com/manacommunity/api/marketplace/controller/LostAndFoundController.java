package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.LostAndFoundRequest;
import com.manacommunity.api.marketplace.dto.LostAndFoundResponse;
import com.manacommunity.api.marketplace.service.LostAndFoundService;
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

import java.util.List;

@RestController
@RequestMapping("/api/marketplace/lost-found")
@RequiredArgsConstructor
public class LostAndFoundController {

    private final LostAndFoundService lostAndFoundService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<LostAndFoundResponse>> getPosts(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(Page.empty());
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(lostAndFoundService.getCommunityPosts(communityId, type, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<LostAndFoundResponse>> getMyPosts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(lostAndFoundService.getMyPosts(user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<LostAndFoundResponse> create(
            @Valid @RequestBody LostAndFoundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lostAndFoundService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<LostAndFoundResponse> resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(lostAndFoundService.resolve(id, user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Marketplace')")
    public ResponseEntity<Void> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        lostAndFoundService.close(id);
        return ResponseEntity.noContent().build();
    }
}
