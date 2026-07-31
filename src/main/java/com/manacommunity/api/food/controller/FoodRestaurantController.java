package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.FoodRestaurantService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/restaurants")
@RequiredArgsConstructor
public class FoodRestaurantController {

    private final FoodRestaurantService restaurantService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Restaurants')")
    public ResponseEntity<Page<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(restaurantService.list(communityId, status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Food Restaurants')")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(restaurantService.getById(communityId, id));
    }

    @GetMapping("/my-restaurant")
    @PreAuthorize("hasAuthority('View Food Restaurants')")
    public ResponseEntity<Map<String, Object>> getMyRestaurant(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(restaurantService.getMyRestaurant(communityId, user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Food Restaurants')")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.create(communityId, request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Food Restaurants')")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(restaurantService.update(communityId, id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Food Restaurants')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(restaurantService.updateStatus(communityId, id, status));
    }

    @GetMapping("/featured")
    @PreAuthorize("hasAuthority('View Food Restaurants')")
    public ResponseEntity<?> getFeatured(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(restaurantService.getFeatured(communityId));
    }
}
