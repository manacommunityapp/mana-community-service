package com.manacommunity.api.fulfillment.controller;

import com.manacommunity.api.fulfillment.dto.ListingDtos.*;
import com.manacommunity.api.fulfillment.service.CommunitySellerListingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/community-listings", "/api/v1/community-listings"})
@RequiredArgsConstructor
public class CommunitySellerListingController {

    private final CommunitySellerListingService listingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> getListings(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(listingService.getListingsByCommunity(communityId, category, pageable));
    }

    @PostMapping
    public ResponseEntity<ListingResponse> addListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateListingRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(listingService.addListing(user.getId(), req));
    }

    @PatchMapping("/{listingId}/toggle")
    public ResponseEntity<ListingResponse> toggleAvailability(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(listingService.toggleAvailability(listingId, user.getId()));
    }
}
