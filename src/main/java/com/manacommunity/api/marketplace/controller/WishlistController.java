package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.WishlistResponse;
import com.manacommunity.api.marketplace.service.WishlistService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<WishlistResponse>> getMyWishlist(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(wishlistService.getMyWishlist(user.getId()));
    }

    @PostMapping("/{listingId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<WishlistResponse> add(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.addToWishlist(user.getId(), listingId, user));
    }

    @DeleteMapping("/{listingId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Void> remove(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        wishlistService.removeFromWishlist(user.getId(), listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{listingId}/check")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Map<String, Boolean>> check(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("wishlisted", wishlistService.isWishlisted(user.getId(), listingId)));
    }
}
