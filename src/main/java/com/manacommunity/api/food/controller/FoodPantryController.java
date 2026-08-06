package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodPantryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/food/pantry")
@RequiredArgsConstructor
public class FoodPantryController {

    private final FoodPantryService pantryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('View Food Pantry')")
    public ResponseEntity<?> getItems(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.getItems(communityId, user.getId()));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(pantryService.addItem(communityId, request, user));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.updateItem(communityId, id, request));
    }

    @PostMapping("/items/{id}/consume")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> consume(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        BigDecimal quantityUsed = new BigDecimal(request.get("quantityUsed").toString());
        String usedFor = (String) request.get("usedFor");
        return ResponseEntity.ok(pantryService.consume(communityId, id, quantityUsed, usedFor, user));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('View Food Pantry')")
    public ResponseEntity<?> getExpiring(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam(defaultValue = "7") int daysAhead) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.getExpiring(communityId, user.getId(), daysAhead));
    }

    @GetMapping("/shopping-lists")
    @PreAuthorize("hasAuthority('View Food Pantry')")
    public ResponseEntity<?> getLists(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.getLists(communityId, user.getId()));
    }

    @PostMapping("/shopping-lists")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        String name = (String) request.get("name");
        return ResponseEntity.status(HttpStatus.CREATED).body(pantryService.create(communityId, name, user));
    }

    @PostMapping("/shopping-lists/{id}/items")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.addItem(communityId, id, request));
    }

    @PatchMapping("/shopping-lists/items/{id}/purchased")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> markPurchased(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        BigDecimal purchasedPrice = new BigDecimal(request.get("purchasedPrice").toString());
        return ResponseEntity.ok(pantryService.markPurchased(communityId, id, purchasedPrice));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('View Food Pantry')")
    public ResponseEntity<?> getAlerts(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.getAlerts(communityId, user.getId()));
    }

    @PatchMapping("/alerts/{id}/read")
    @PreAuthorize("hasAuthority('Manage Food Pantry')")
    public ResponseEntity<?> markRead(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(pantryService.markRead(communityId, id));
    }
}
