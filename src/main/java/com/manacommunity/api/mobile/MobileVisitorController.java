package com.manacommunity.api.mobile;

import com.manacommunity.api.model.Community;
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

/**
 * Mobile-friendly visitor management endpoints.
 * Uses isAuthenticated() so all residents can manage their own visitor passes
 * without needing the role-based authorities required by VisitorPassController.
 */
@RestController
@RequestMapping("/api/visitors/mobile")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MobileVisitorController {

    private final VisitorPassService visitorPassService;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/visitors/mobile/my-visitors — resident's own passes */
    @GetMapping("/my-visitors")
    public ResponseEntity<List<VisitorPassResponse>> getMyVisitors(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.getMyPasses(user.getId()));
    }

    /** GET /api/visitors/mobile/pending — passes pending resident approval */
    @GetMapping("/pending")
    public ResponseEntity<List<VisitorPassResponse>> getPending(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.getPendingApprovals(user.getId()));
    }

    /** GET /api/visitors/mobile/{id} — single pass by id (must belong to resident) */
    @GetMapping("/{id}")
    public ResponseEntity<VisitorPassResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        VisitorPassResponse pass = visitorPassService.getById(id);
        if (!user.getId().equals(pass.getResidentId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(pass);
    }

    /** POST /api/visitors/mobile/pre-approve — create a pre-approved visitor pass */
    @PostMapping("/pre-approve")
    public ResponseEntity<VisitorPassResponse> preApprove(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VisitorPassRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Community community = user.getCommunity();
        if (community == null) {
            return ResponseEntity.badRequest().build();
        }
        VisitorPassResponse created = visitorPassService.create(req, user, community);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/visitors/mobile/{id}/approve — resident approves a pending walk-in */
    @PutMapping("/{id}/approve")
    public ResponseEntity<VisitorPassResponse> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        VisitorPassResponse pass = visitorPassService.getById(id);
        if (!user.getId().equals(pass.getResidentId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(visitorPassService.approvePass(id, user));
    }

    /** PUT /api/visitors/mobile/{id}/reject — resident rejects a pass */
    @PutMapping("/{id}/reject")
    public ResponseEntity<VisitorPassResponse> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        VisitorPassResponse pass = visitorPassService.getById(id);
        if (!user.getId().equals(pass.getResidentId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(visitorPassService.rejectPass(id, user.getFullName()));
    }

    /** PUT /api/visitors/mobile/{id}/check-in — resident marks visitor checked in */
    @PutMapping("/{id}/check-in")
    public ResponseEntity<VisitorPassResponse> checkIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        VisitorPassResponse pass = visitorPassService.getById(id);
        if (!user.getId().equals(pass.getResidentId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(visitorPassService.checkIn(id));
    }

    /** PUT /api/visitors/mobile/{id}/check-out — resident marks visitor checked out */
    @PutMapping("/{id}/check-out")
    public ResponseEntity<VisitorPassResponse> checkOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        VisitorPassResponse pass = visitorPassService.getById(id);
        if (!user.getId().equals(pass.getResidentId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(visitorPassService.checkOut(id));
    }
}
