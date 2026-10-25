package com.manacommunity.api.sports.controller;

import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.SportsTrainerService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.permissions.SportsPermissions.*;

/**
 * Sports Trainer endpoints:
 *
 * <ul>
 *   <li>GET  /api/sports/trainers             — list approved trainers (community-scoped)</li>
 *   <li>POST /api/sports/trainers/apply        — resident applies as trainer</li>
 *   <li>GET  /api/sports/trainers/admin        — admin: all applications</li>
 *   <li>PATCH /api/sports/trainers/{id}/status — admin: approve / reject</li>
 *   <li>GET  /api/sports/trainers/{id}/slots   — open slots for a trainer</li>
 *   <li>POST /api/sports/trainer-slots         — trainer/admin creates a slot</li>
 *   <li>POST /api/sports/trainer-slots/{id}/book — resident books a slot</li>
 *   <li>POST /api/sports/trainer-bookings/{id}/cancel — resident cancels</li>
 *   <li>GET  /api/sports/trainer-bookings/my   — resident's own bookings</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsTrainerController {

    private final SportsTrainerService trainerService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    // ── Discovery ─────────────────────────────────────────────────────────────

    @GetMapping("/trainers")
    public ResponseEntity<List<SportsTrainerResponse>> listApproved(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(trainerService.listApproved(communityId));
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    @PostMapping("/trainers/apply")
    public ResponseEntity<SportsTrainerResponse> apply(
            @RequestParam Long communityId,
            @RequestBody SportsTrainerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainerService.apply(communityId, user.getId(), request));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/trainers/admin")
    public ResponseEntity<List<SportsTrainerResponse>> listAll(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(trainerService.listAll(communityId));
    }

    @PatchMapping("/trainers/{id}/status")
    public ResponseEntity<SportsTrainerResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        String status = body.get("status");
        String reason = body.get("rejectionReason");
        return ResponseEntity.ok(trainerService.updateStatus(id, status, admin.getId(), reason));
    }

    // ── Slots ─────────────────────────────────────────────────────────────────

    @GetMapping("/trainers/{id}/slots")
    public ResponseEntity<List<TrainerSlotResponse>> listSlots(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(trainerService.listSlots(id));
    }

    @PostMapping("/trainer-slots")
    public ResponseEntity<TrainerSlotResponse> createSlot(
            @RequestParam Long communityId,
            @RequestBody TrainerSlotRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainerService.createSlot(communityId, request));
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    @PostMapping("/trainer-slots/{slotId}/book")
    public ResponseEntity<TrainerSlotBookingResponse> bookSlot(
            @PathVariable Long slotId,
            @RequestBody TrainerSlotBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainerService.bookSlot(slotId, user.getId(), request));
    }

    @PostMapping("/trainer-bookings/{bookingId}/cancel")
    public ResponseEntity<TrainerSlotBookingResponse> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(trainerService.cancelBooking(bookingId, user.getId()));
    }

    @GetMapping("/trainer-bookings/my")
    public ResponseEntity<List<TrainerSlotBookingResponse>> myBookings(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(trainerService.myBookings(user.getId(), communityId));
    }
}
