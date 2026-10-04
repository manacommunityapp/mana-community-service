package com.manacommunity.api.controller;

import com.manacommunity.api.dto.GroceryItemResponse;
import com.manacommunity.api.dto.GroceryOrderRequest;
import com.manacommunity.api.dto.GroceryOrderResponse;
import com.manacommunity.api.service.GroceryService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food/grocery")
@RequiredArgsConstructor
public class GroceryController {

    private final GroceryService groceryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/items")
    public ResponseEntity<List<GroceryItemResponse>> getItems(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(groceryService.getAvailableItems(communityId(principal)));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<GroceryItemResponse> getItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(groceryService.getItem(id, communityId(principal)));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<GroceryOrderResponse>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(groceryService.getUserOrders(user.getId()));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<GroceryOrderResponse> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(groceryService.getOrder(id, user.getId()));
    }

    @PostMapping("/orders")
    public ResponseEntity<GroceryOrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GroceryOrderRequest request) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(groceryService.createOrder(request, user));
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<GroceryOrderResponse> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = requireMember(principal);
        return ResponseEntity.ok(groceryService.cancelOrder(id, user.getId()));
    }

    private Long communityId(UserPrincipal principal) {
        return requireMember(principal).getCommunity().getId();
    }

    private AppUser requireMember(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new IllegalArgumentException("Grocery features are scoped to a community.");
        }
        return user;
    }
}
