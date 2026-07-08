package com.manacommunity.api.visitor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.service.VisitorPassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorPassController {

    private final VisitorPassService visitorPassService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Visitors')")
    public ResponseEntity<List<VisitorPassResponse>> getCommunityPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(visitorPassService.getCommunityPasses(communityId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<List<VisitorPassResponse>> getActivePasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(visitorPassService.getActivePasses(communityId));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<List<VisitorPassResponse>> getTodaysPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(visitorPassService.getTodaysPasses(communityId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Visitors')")
    public ResponseEntity<List<VisitorPassResponse>> getMyPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.getMyPasses(user.getId()));
    }

    @GetMapping("/code/{passCode}")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> getByPassCode(@PathVariable String passCode) {
        return ResponseEntity.ok(visitorPassService.getByPassCode(passCode));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Visitors')")
    public ResponseEntity<VisitorPassResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(visitorPassService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Visitor Pass')")
    public ResponseEntity<VisitorPassResponse> create(
            @Valid @RequestBody VisitorPassRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(visitorPassService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(visitorPassService.checkIn(id));
    }

    @PutMapping("/code/{passCode}/check-in")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> checkInByCode(@PathVariable String passCode) {
        return ResponseEntity.ok(visitorPassService.checkInByCode(passCode));
    }

    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(visitorPassService.checkOut(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        visitorPassService.reject(id);
        return ResponseEntity.ok().build();
    }
}
