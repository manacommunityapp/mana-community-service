package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodGroceryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/grocery")
@RequiredArgsConstructor
public class FoodGroceryController {

    private final FoodGroceryService groceryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getStores(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(groceryService.getStores(communityId, status, pageable));
    }

    @GetMapping("/stores/{id}")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getStoreById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.getStoreById(communityId, id));
    }

    @PostMapping("/stores")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> createStore(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groceryService.createStore(communityId, request));
    }

    @PutMapping("/stores/{id}")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.updateStore(communityId, id, request));
    }

    @GetMapping("/stores/{storeId}/categories")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getCategories(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.getCategories(communityId, storeId));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getProducts(
            @RequestParam Long storeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(groceryService.getProducts(communityId, storeId, categoryId, search, pageable));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getProductById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.getProductById(communityId, id));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groceryService.createProduct(communityId, request));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.updateProduct(communityId, id, request));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> placeOrder(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groceryService.placeOrder(communityId, request));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(groceryService.getMyOrders(communityId, user.getId(), pageable));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.getOrderById(communityId, id));
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.updateStatus(communityId, id, status));
    }

    @GetMapping("/delivery-slots")
    @PreAuthorize("hasAuthority('View Food Grocery')")
    public ResponseEntity<?> getDeliverySlots(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.getDeliverySlots(communityId, storeId, date));
    }

    @PostMapping("/wishlist")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> addToWishlist(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groceryService.addToWishlist(communityId, user.getId(), request));
    }

    @DeleteMapping("/wishlist/{productId}")
    @PreAuthorize("hasAuthority('Manage Food Grocery')")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        groceryService.removeFromWishlist(communityId, user.getId(), productId);
        return ResponseEntity.noContent().build();
    }
}
