package com.manacommunity.api.mw.controller;

import com.manacommunity.api.mw.dto.*;
import com.manacommunity.api.mw.service.MwSkillPassportService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mw/passport")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MwSkillPassportController {

    private final MwSkillPassportService passportService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyPassport(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(passportService.getMyPassport(user.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updatePassport(
            @RequestBody PassportUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(passportService.updatePassport(user.getId(), req));
    }

    @PostMapping("/skills")
    public ResponseEntity<?> addSkill(
            @RequestBody SkillRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(passportService.addSkill(user.getId(), req));
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<?> removeSkill(
            @PathVariable Long skillId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        passportService.removeSkill(user.getId(), skillId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/certifications")
    public ResponseEntity<?> addCertification(
            @RequestBody CertificationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(passportService.addCertification(user.getId(), req));
    }

    @DeleteMapping("/certifications/{certId}")
    public ResponseEntity<?> removeCertification(
            @PathVariable Long certId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        passportService.removeCertification(user.getId(), certId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/work-history")
    public ResponseEntity<?> addWorkHistory(
            @RequestBody WorkHistoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(passportService.addWorkHistory(user.getId(), req));
    }

    @DeleteMapping("/work-history/{whId}")
    public ResponseEntity<?> removeWorkHistory(
            @PathVariable Long whId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        passportService.removeWorkHistory(user.getId(), whId);
        return ResponseEntity.noContent().build();
    }

    // Public profile view — no auth required for this specific endpoint
    @GetMapping("/{userId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getPublicPassport(@PathVariable Long userId) {
        return ResponseEntity.ok(passportService.getPublicPassport(userId));
    }

    // Skills endpoint for inter-module calls (availability engine etc.)
    @GetMapping("/{userId}/skills")
    public ResponseEntity<?> getUserSkills(@PathVariable Long userId) {
        return ResponseEntity.ok(passportService.getUserSkills(userId));
    }
}
