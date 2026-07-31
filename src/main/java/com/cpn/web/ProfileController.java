package com.cpn.web;

import com.cpn.application.profile.ProfileService;
import com.cpn.domain.profile.model.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<Profile> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfileByUserId(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Profile> updateProfile(@PathVariable UUID userId, @RequestBody Profile profile) {
        return ResponseEntity.ok(profileService.updateProfile(userId, profile));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Profile>> searchProfiles(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(profileService.searchProfiles(q));
    }
}
