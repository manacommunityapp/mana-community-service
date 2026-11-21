package com.manacommunity.api.trip.p2p.controller;

import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.p2p.dto.TripP2PDtos.CreateP2PDebtRequest;
import com.manacommunity.api.trip.p2p.dto.TripP2PDtos.InitiateCollectRequest;
import com.manacommunity.api.trip.p2p.dto.TripP2PDtos.TripP2PCollectResponse;
import com.manacommunity.api.trip.p2p.entity.TripP2PCollectRequest;
import com.manacommunity.api.trip.p2p.service.TripP2PCollectService;
import com.manacommunity.api.trip.repository.TripRepository;
import com.manacommunity.api.trip.split.service.TripSplitAccess;
import com.manacommunity.api.user.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * P2P UPI Collect endpoints for community trip cost-sharing.
 *
 * <p>The trip organizer (or community admin) creates debt records and initiates
 * UPI Collect requests. The debtor approves on their UPI app. Settlement is
 * confirmed via the Razorpay webhook.
 *
 * <p>TRUST: amount is never accepted from the client for initiation — it is
 * read from the server-stored debt record created by the organizer.
 */
@Slf4j
@Tag(name = "Trip P2P Collect")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TripP2PCollectController {

    private final TripP2PCollectService collectService;
    private final TripRepository tripRepo;
    private final TripSplitAccess tripSplitAccess;

    @Operation(summary = "Create a debt record for a trip participant (organizer only)")
    @PostMapping("/{tripId}/p2p/debts")
    public ResponseEntity<TripP2PCollectResponse> createDebt(
            @PathVariable String tripId,
            @RequestBody @Valid CreateP2PDebtRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        TripP2PCollectRequest record = collectService.createDebt(
                tripId, principal.getUserId(), req, principal.getCommunityId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TripP2PCollectResponse.from(record));
    }

    @Operation(summary = "Initiate UPI Collect request for a debt (creditor only)")
    @PostMapping("/p2p/debts/{debtId}/collect")
    public ResponseEntity<TripP2PCollectResponse> initiateCollect(
            @PathVariable Long debtId,
            @RequestBody @Valid InitiateCollectRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        TripP2PCollectRequest record = collectService.initiateCollect(
                debtId, req.getUpiVpa(), principal.getUserId());
        return ResponseEntity.ok(TripP2PCollectResponse.from(record));
    }

    @Operation(summary = "List all debt records for a trip")
    @GetMapping("/{tripId}/p2p/debts")
    public ResponseEntity<List<TripP2PCollectResponse>> listDebts(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId));

        boolean isOrganizerOrAdmin = tripSplitAccess.isOrganizer(trip, principal.getUserId())
                || hasAdminRole(principal);

        List<TripP2PCollectRequest> records = collectService.listForTrip(
                tripId, principal.getUserId(), isOrganizerOrAdmin);

        return ResponseEntity.ok(records.stream().map(TripP2PCollectResponse::from).toList());
    }

    @Operation(summary = "Cancel a pending collect request")
    @DeleteMapping("/p2p/debts/{debtId}")
    public ResponseEntity<Void> cancelDebt(
            @PathVariable Long debtId,
            @AuthenticationPrincipal UserPrincipal principal) {

        collectService.cancelDebt(debtId, principal.getUserId(), hasAdminRole(principal));
        return ResponseEntity.noContent().build();
    }

    private boolean hasAdminRole(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> {
                    String role = a.getAuthority();
                    return "ROLE_ADMIN".equals(role)
                            || "ROLE_SUPER_ADMIN".equals(role)
                            || "ROLE_COMMUNITY_ADMIN".equals(role)
                            || "ROLE_FINANCE_MGMT_ADMIN".equals(role);
                });
    }
}
