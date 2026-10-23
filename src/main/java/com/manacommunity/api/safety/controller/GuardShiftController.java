package com.manacommunity.api.safety.controller;

import com.manacommunity.api.safety.dto.GuardShiftRequest;
import com.manacommunity.api.safety.dto.GuardShiftResponse;
import com.manacommunity.api.safety.dto.ShiftStatusRequest;
import com.manacommunity.api.safety.service.GuardShiftService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety/shifts")
@RequiredArgsConstructor
public class GuardShiftController {

    private final GuardShiftService shiftService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<GuardShiftResponse>> getShifts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(shiftService.getShifts(communityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuardShiftResponse> getShiftById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<GuardShiftResponse> createShift(
            @Valid @RequestBody GuardShiftRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftService.createShift(request, communityId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<GuardShiftResponse> updateShiftStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShiftStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(shiftService.updateShiftStatus(id, request.getStatus()));
    }
}
