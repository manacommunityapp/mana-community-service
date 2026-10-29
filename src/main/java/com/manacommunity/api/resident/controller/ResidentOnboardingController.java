package com.manacommunity.api.resident.controller;

import com.manacommunity.api.resident.dto.*;
import com.manacommunity.api.resident.service.ResidentOnboardingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentOnboardingController {

    private final ResidentOnboardingService residentOnboardingService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/onboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResidentOnboardingResponse> onboardResident(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ResidentOnboardingRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        ResidentOnboardingResponse response = residentOnboardingService.onboardResident(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/flats/{flatId}/family")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyRosterResponse> getFamilyRoster(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long flatId) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        FamilyRosterResponse response = residentOnboardingService.getFamilyRoster(flatId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/family/adult")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyRosterResponse> addAdultMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddAdultMemberRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        FamilyRosterResponse response = residentOnboardingService.addAdultMember(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/family/child")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyRosterResponse> addChildDependent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddChildMemberRequest request) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        FamilyRosterResponse response = residentOnboardingService.addChildDependent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/properties")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserPropertySummaryDto>> getUserProperties(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        List<UserPropertySummaryDto> properties = residentOnboardingService.getUserProperties(currentUser);
        return ResponseEntity.ok(properties);
    }

    @DeleteMapping("/family/dependent/{dependentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeDependent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dependentId) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        residentOnboardingService.removeDependent(dependentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/family/adult/{membershipId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeAdultMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long membershipId) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        residentOnboardingService.removeAdultMember(membershipId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
