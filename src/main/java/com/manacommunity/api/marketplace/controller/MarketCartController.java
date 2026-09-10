package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketCartDto;
import com.manacommunity.api.marketplace.service.MarketCartService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/marketplace/cart", "/api/v1/marketplace/cart"})
@RequiredArgsConstructor
public class MarketCartController {

    private final MarketCartService cartService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketCartDto.Response> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(cartService.getCart(user));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketCartDto.Response> addItem(
            @Valid @RequestBody MarketCartDto.AddItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(cartService.addItem(req, user));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketCartDto.Response> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody MarketCartDto.UpdateItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(cartService.updateItem(itemId, req, user));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketCartDto.Response> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(cartService.removeItem(itemId, user));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}
