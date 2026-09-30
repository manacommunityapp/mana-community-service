package com.manacommunity.api.guard.controller;

import com.manacommunity.api.guard.dto.*;
import com.manacommunity.api.guard.service.GuardService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.MANAGE_GUARDS;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_GUARDS;

@RestController
@RequestMapping("/api/guards")
@RequiredArgsConstructor
public class GuardController {

    private final GuardService guardService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    // ── Guard Profiles ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<GuardProfileResponse>> getGuards(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_GUARDS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(guardService.getGuards(caller.getCommunity().getId()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<GuardProfileResponse>> getActiveGuards(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_GUARDS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(guardService.getActiveGuards(caller.getCommunity().getId()));
    }

    @PostMapping
    public ResponseEntity<GuardProfileResponse> createGuard(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GuardProfileRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardService.createGuard(caller, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuardProfileResponse> updateGuard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GuardProfileRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        return ResponseEntity.ok(guardService.updateGuard(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<GuardProfileResponse> updateGuardStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam String status) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        return ResponseEntity.ok(guardService.updateGuardStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        guardService.deleteGuard(id);
        return ResponseEntity.noContent().build();
    }

    // ── Guard Shifts ────────────────────────────────────────────────────

    @GetMapping("/shifts")
    public ResponseEntity<List<GuardShiftResponse>> getShifts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        permissionCheckService.requireAnyPermission(principal, VIEW_GUARDS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(guardService.getShiftsByDate(caller.getCommunity().getId(), date));
    }

    @GetMapping("/shifts/range")
    public ResponseEntity<List<GuardShiftResponse>> getShiftsByRange(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        permissionCheckService.requireAnyPermission(principal, VIEW_GUARDS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(guardService.getShiftsByRange(caller.getCommunity().getId(), from, to));
    }

    @GetMapping("/{guardId}/shifts")
    public ResponseEntity<List<GuardShiftResponse>> getGuardShifts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long guardId) {
        permissionCheckService.requireAnyPermission(principal, VIEW_GUARDS);
        return ResponseEntity.ok(guardService.getGuardShifts(guardId));
    }

    @PostMapping("/shifts")
    public ResponseEntity<GuardShiftResponse> createShift(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GuardShiftRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardService.createShift(caller, request));
    }

    @PutMapping("/shifts/{id}")
    public ResponseEntity<GuardShiftResponse> updateShift(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GuardShiftRequest request) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        return ResponseEntity.ok(guardService.updateShift(id, request));
    }

    @PostMapping("/shifts/{id}/check-in")
    public ResponseEntity<GuardShiftResponse> checkInShift(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        return ResponseEntity.ok(guardService.checkInShift(id));
    }

    @PostMapping("/shifts/{id}/check-out")
    public ResponseEntity<GuardShiftResponse> checkOutShift(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        return ResponseEntity.ok(guardService.checkOutShift(id));
    }

    @DeleteMapping("/shifts/{id}")
    public ResponseEntity<Void> deleteShift(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, MANAGE_GUARDS);
        guardService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }
}
