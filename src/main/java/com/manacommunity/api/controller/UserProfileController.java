package com.manacommunity.api.controller;

import com.manacommunity.api.dto.UserProfileRequest;
import com.manacommunity.api.dto.UserProfileResponse;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.UserProfileService;
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

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse response = userProfileService.getProfile(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserProfileRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse response = userProfileService.updateProfile(user, request);
        return ResponseEntity.ok(response);
    }
}
