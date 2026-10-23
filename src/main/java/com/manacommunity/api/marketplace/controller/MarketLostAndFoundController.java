package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketLostAndFoundRequest;
import com.manacommunity.api.marketplace.dto.MarketLostAndFoundResponse;
import com.manacommunity.api.marketplace.entity.MarketLostAndFound;
import com.manacommunity.api.marketplace.service.MarketLostAndFoundService;
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
@RequestMapping({"/api/marketplace/lost-and-found", "/api/v1/marketplace/lost-and-found"})
@RequiredArgsConstructor
public class MarketLostAndFoundController {

    private final MarketLostAndFoundService lostAndFoundService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<MarketLostAndFoundResponse>> getPosts(
            @RequestParam(required = false) MarketLostAndFound.PostType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(lostAndFoundService.getCommunityPosts(communityId, type, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketLostAndFoundResponse>> getMyPosts(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(lostAndFoundService.getMyPosts(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketLostAndFoundResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(lostAndFoundService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketLostAndFoundResponse> create(
            @Valid @RequestBody MarketLostAndFoundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lostAndFoundService.create(req, user, user.getCommunity()));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketLostAndFoundResponse> claim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(lostAndFoundService.claimItem(id, user));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketLostAndFoundResponse> resolvePost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(lostAndFoundService.resolvePost(id));
    }
}
