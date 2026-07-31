package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodRecipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/recipes")
@RequiredArgsConstructor
public class FoodRecipeController {

    private final FoodRecipeService recipeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Recipes')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(recipeService.list(communityId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Food Recipes')")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.getById(communityId, id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('View Food Recipes')")
    public ResponseEntity<?> getMyRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(recipeService.getMyRecipes(communityId, user.getId(), pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> create(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recipeService.create(communityId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.update(communityId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        recipeService.delete(communityId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> rate(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.rate(communityId, id, request));
    }

    @GetMapping("/collections")
    @PreAuthorize("hasAuthority('View Food Recipes')")
    public ResponseEntity<?> getCollections(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.getCollections(communityId, user.getId()));
    }

    @PostMapping("/collections")
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> createCollection(
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.createCollection(communityId, request));
    }

    @PostMapping("/collections/{id}/items")
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> addToCollection(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.addToCollection(communityId, id, request));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('View Food Recipes')")
    public ResponseEntity<?> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(recipeService.getComments(communityId, id, pageable));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('Manage Food Recipes')")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(recipeService.addComment(communityId, id, request));
    }
}
