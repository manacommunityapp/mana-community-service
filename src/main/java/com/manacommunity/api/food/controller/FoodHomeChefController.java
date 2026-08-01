package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.FoodHomeChefService;
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
@RequestMapping("/api/food/home-chefs")
@RequiredArgsConstructor
public class FoodHomeChefController {

    private final FoodHomeChefService homeChefService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Home Chefs')")
    public ResponseEntity<Page<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(homeChefService.list(communityId, status, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Food Home Chefs')")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(homeChefService.getById(communityId, id));
    }

    @GetMapping("/my-profile")
    @PreAuthorize("hasAuthority('View Food Home Chefs')")
    public ResponseEntity<Map<String, Object>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(homeChefService.getMyProfile(communityId, user.getId()));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('Manage Food Home Chefs')")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homeChefService.register(communityId, request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Food Home Chefs')")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(homeChefService.update(communityId, id, request));
    }

    @GetMapping("/{id}/menu")
    @PreAuthorize("hasAuthority('View Food Home Chefs')")
    public ResponseEntity<Page<Map<String, Object>>> getMenu(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(homeChefService.getMenu(communityId, id, pageable));
    }

    @PostMapping("/{id}/menu")
    @PreAuthorize("hasAuthority('Manage Food Home Chefs')")
    public ResponseEntity<Map<String, Object>> addMenuItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(homeChefService.addMenuItem(communityId, id, request));
    }

    @PutMapping("/menu/{menuId}")
    @PreAuthorize("hasAuthority('Manage Food Home Chefs')")
    public ResponseEntity<Map<String, Object>> updateMenuItem(
            @PathVariable Long menuId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(homeChefService.updateMenuItem(communityId, menuId, request));
    }

    @GetMapping("/{id}/reviews")
    @PreAuthorize("hasAuthority('View Food Home Chefs')")
    public ResponseEntity<Page<Map<String, Object>>> getReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(homeChefService.getReviews(communityId, id, pageable));
    }

    @GetMapping("/{id}/payouts")
    @PreAuthorize("hasAuthority('Manage Food Home Chefs')")
    public ResponseEntity<Page<Map<String, Object>>> getPayouts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(homeChefService.getPayouts(communityId, id, pageable));
    }
}
