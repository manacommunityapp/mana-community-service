package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.ListingRequest;
import com.manacommunity.api.marketplace.dto.ListingResponse;
import com.manacommunity.api.marketplace.service.ListingService;
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
@RequestMapping("/api/marketplace/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<ListingResponse>> getListings(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(List.of());
        }
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(listingService.searchListings(communityId, search));
        }
        return ResponseEntity.ok(listingService.getCommunityListings(communityId, category));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<ListingResponse>> getMyListings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(listingService.getMyListings(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<ListingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<ListingResponse> create(
            @Valid @RequestBody ListingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<ListingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(listingService.update(id, req, user.getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        listingService.updateStatus(id, status, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Delete Listing')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        listingService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }
}
