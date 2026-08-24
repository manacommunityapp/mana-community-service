package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.FoodMenuService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/restaurants/{restaurantId}/menu")
@RequiredArgsConstructor
public class FoodMenuController {

    private final FoodMenuService menuService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Restaurants', 'View Food Menu')")
    public ResponseEntity<?> getCategories(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.getCategories(communityId, restaurantId));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Menu')")
    public ResponseEntity<Map<String, Object>> createCategory(
            @PathVariable Long restaurantId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.createCategory(communityId, restaurantId, request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Menu')")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.updateCategory(communityId, restaurantId, id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Menu')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        menuService.deleteCategory(communityId, restaurantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Restaurants', 'View Food Menu')")
    public ResponseEntity<Page<Map<String, Object>>> getItems(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(menuService.getItems(communityId, restaurantId, categoryId, search, pageable));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Restaurants', 'View Food Menu')")
    public ResponseEntity<Map<String, Object>> getItem(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.getItem(communityId, restaurantId, id));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Menu')")
    public ResponseEntity<Map<String, Object>> createItem(
            @PathVariable Long restaurantId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.createItem(communityId, restaurantId, request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Menu')")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.updateItem(communityId, restaurantId, id, request));
    }

    @PatchMapping("/items/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Menu')")
    public ResponseEntity<Map<String, Object>> toggleAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @RequestParam boolean available,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.toggleAvailability(communityId, restaurantId, id, available));
    }

    @GetMapping("/combos")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Restaurants', 'View Food Menu')")
    public ResponseEntity<?> getCombos(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(menuService.getCombos(communityId, restaurantId));
    }
}
