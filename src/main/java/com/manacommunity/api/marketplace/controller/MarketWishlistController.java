package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketWishlistResponse;
import com.manacommunity.api.marketplace.service.MarketWishlistService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/marketplace/wishlist", "/api/v1/marketplace/wishlist"})
@RequiredArgsConstructor
public class MarketWishlistController {

    private final MarketWishlistService wishlistService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketWishlistResponse>> getWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(wishlistService.getWishlist(user.getId()));
    }

    @GetMapping("/check/{listingId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Map<String, Boolean>> isWishlisted(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("wishlisted", wishlistService.isWishlisted(user.getId(), listingId)));
    }

    @PostMapping("/toggle/{listingId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Void> toggleWishlist(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        wishlistService.toggleWishlist(listingId, user);
        return ResponseEntity.ok().build();
    }
}
