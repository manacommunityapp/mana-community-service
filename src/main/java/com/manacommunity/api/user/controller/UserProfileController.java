package com.manacommunity.api.user.controller;

import com.manacommunity.api.user.dto.UserProfileRequest;
import com.manacommunity.api.user.dto.UserProfileResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final LoggedInUserService loggedInUserService;
    private final UserProfileService userProfileService;
    private final AppUserRepository appUserRepository;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse response = userProfileService.getProfile(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<UserProfileResponse.UserStats> getProfileStats(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse.UserStats response = userProfileService.getProfileStats(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserProfileRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse response = userProfileService.updateProfile(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activities")
    public ResponseEntity<java.util.List<UserProfileResponse.UserActivityItem>> getProfileActivities(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        java.util.List<UserProfileResponse.UserActivityItem> activities = userProfileService.getUserActivities(user);
        return ResponseEntity.ok(activities);
    }

    @PutMapping("/occupancy-type")
    public ResponseEntity<java.util.Map<String, String>> updateOccupancyType(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody OccupancyTypeRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        user.setOccupancyStatus(request.getType());
        appUserRepository.save(user);
        return ResponseEntity.ok(java.util.Map.of("occupancyStatus", request.getType()));
    }

    @Data
    public static class OccupancyTypeRequest {
        private String type;
    }
}
