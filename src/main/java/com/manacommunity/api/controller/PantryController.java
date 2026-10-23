package com.manacommunity.api.controller;

import com.manacommunity.api.dto.PantryItemRequest;
import com.manacommunity.api.dto.PantryItemResponse;
import com.manacommunity.api.service.PantryService;
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
@RequestMapping("/api/food/pantry")
@RequiredArgsConstructor
public class PantryController {

    private final PantryService pantryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/items")
    public ResponseEntity<List<PantryItemResponse>> getItems(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(pantryService.getUserPantryItems(userId));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<PantryItemResponse> getItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(pantryService.getPantryItem(id, userId));
    }

    @PostMapping("/items")
    public ResponseEntity<PantryItemResponse> createItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PantryItemRequest request) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(pantryService.createPantryItem(request, userId));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<PantryItemResponse> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PantryItemRequest request) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(pantryService.updatePantryItem(id, request, userId));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long userId = resolveUserId(principal);
        pantryService.deletePantryItem(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/expiring-soon")
    public ResponseEntity<List<PantryItemResponse>> getExpiringSoon(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(pantryService.getExpiringSoonItems(userId));
    }

    private Long resolveUserId(UserPrincipal principal) {
        return loggedInUserService.resolve(principal).getId();
    }
}
