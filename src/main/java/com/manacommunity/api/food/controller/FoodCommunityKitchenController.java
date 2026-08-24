package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodCommunityKitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/community-kitchens")
@RequiredArgsConstructor
public class FoodCommunityKitchenController {

    private final FoodCommunityKitchenService communityKitchenService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Community Kitchen', 'Manage Food Community Kitchen', 'View Food Menu', 'View Food Profile')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.list(communityId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Community Kitchen', 'Manage Food Community Kitchen', 'View Food Menu', 'View Food Profile')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.getById(communityId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Community Kitchen', 'Manage Food Menu')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(communityKitchenService.create(communityId, request, user));
    }

    @GetMapping("/{id}/menu")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Community Kitchen', 'Manage Food Community Kitchen', 'View Food Menu', 'View Food Profile')")
    public ResponseEntity<?> getMenu(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @RequestParam(required = false) LocalDate date) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.getMenu(communityId, id, date));
    }

    @PostMapping("/{id}/menu")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Community Kitchen', 'Manage Food Menu')")
    public ResponseEntity<?> createMenu(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.createMenu(communityId, id, request));
    }

    @PostMapping("/book")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('Manage Food Community Kitchen', 'View Food Community Kitchen')")
    public ResponseEntity<?> bookMeal(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.bookMeal(communityId, request, user));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Community Kitchen', 'Manage Food Community Kitchen')")
    public ResponseEntity<?> getMyBookings(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.getMyBookings(communityId, user != null ? user.getId() : null, PageRequest.of(page, size)));
    }

    @PostMapping("/verify-token")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Community Kitchen')")
    public ResponseEntity<?> verifyToken(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(communityKitchenService.verifyToken(communityId, request));
    }
}
