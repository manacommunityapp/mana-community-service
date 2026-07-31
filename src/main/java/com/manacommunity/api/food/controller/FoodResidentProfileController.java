package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.service.FoodResidentProfileService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/profile")
@RequiredArgsConstructor
public class FoodResidentProfileController {

    private final FoodResidentProfileService profileService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Profile')")
    public ResponseEntity<Map<String, Object>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(profileService.getMyProfile(communityId, user.getId()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('Manage Food Profile')")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(profileService.updateProfile(communityId, request, user));
    }

    @GetMapping("/allergies")
    @PreAuthorize("hasAuthority('View Food Profile')")
    public ResponseEntity<?> getAllergies(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Long profileId = profileService.getProfileId(communityId, user.getId());
        return ResponseEntity.ok(profileService.getAllergies(profileId));
    }

    @PostMapping("/allergies")
    @PreAuthorize("hasAuthority('Manage Food Profile')")
    public ResponseEntity<Map<String, Object>> addAllergy(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(profileService.addAllergy(communityId, user.getId(), request));
    }

    @DeleteMapping("/allergies/{id}")
    @PreAuthorize("hasAuthority('Manage Food Profile')")
    public ResponseEntity<Void> removeAllergy(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        profileService.removeAllergy(communityId, user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasAuthority('View Food Profile')")
    public ResponseEntity<?> getFavorites(
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(profileService.getFavorites(communityId, user.getId(), type));
    }

    @PostMapping("/favorites")
    @PreAuthorize("hasAuthority('Manage Food Profile')")
    public ResponseEntity<Map<String, Object>> addFavorite(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(profileService.addFavorite(communityId, user.getId(), request));
    }

    @DeleteMapping("/favorites/{id}")
    @PreAuthorize("hasAuthority('Manage Food Profile')")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        profileService.removeFavorite(communityId, user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals")
    @PreAuthorize("hasAuthority('View Food Profile')")
    public ResponseEntity<?> getGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(profileService.getGoals(communityId, user.getId()));
    }
}
