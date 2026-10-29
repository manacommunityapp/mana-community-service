package com.manacommunity.api.mobile;

import com.manacommunity.api.marketplace.dto.MarketListingRequest;
import com.manacommunity.api.marketplace.dto.MarketListingResponse;
import com.manacommunity.api.marketplace.service.MarketListingService;
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

/**
 * Mobile-friendly marketplace listing endpoints.
 * Uses isAuthenticated() so all residents can browse and post listings
 * without needing the role-based 'View Marketplace' / 'Create Listing' authorities.
 */
@RestController
@RequestMapping("/api/marketplace/mobile")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MobileMarketplaceController {

    private final MarketListingService listingService;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/marketplace/mobile/listings — browse community listings */
    @GetMapping("/listings")
    public ResponseEntity<Page<MarketListingResponse>> getListings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(Page.empty());
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(listingService.searchListings(communityId, search, pageable));
        }
        return ResponseEntity.ok(listingService.getCommunityListings(communityId, category, pageable));
    }

    /** GET /api/marketplace/mobile/listings/{id} — single listing */
    @GetMapping("/listings/{id}")
    public ResponseEntity<MarketListingResponse> getListing(
            @PathVariable Long id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    /** GET /api/marketplace/mobile/my-listings — resident's own listings */
    @GetMapping("/my-listings")
    public ResponseEntity<List<MarketListingResponse>> getMyListings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(listingService.getMyListings(user.getId()));
    }

    /** POST /api/marketplace/mobile/listings — create a new listing */
    @PostMapping("/listings")
    public ResponseEntity<MarketListingResponse> createListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MarketListingRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(req, user, user.getCommunity()));
    }

    /** PUT /api/marketplace/mobile/listings/{id} — update own listing */
    @PutMapping("/listings/{id}")
    public ResponseEntity<MarketListingResponse> updateListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MarketListingRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(listingService.update(id, req, user.getId()));
    }

    /** PUT /api/marketplace/mobile/listings/{id}/status — mark listing sold/available */
    @PutMapping("/listings/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam String status) {
        AppUser user = loggedInUserService.resolve(principal);
        listingService.updateStatus(id, status, user.getId());
        return ResponseEntity.ok().build();
    }
}
