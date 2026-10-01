package com.manacommunity.api.visitor.controller;

import com.manacommunity.api.privacy.PiiMaskingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.entity.VisitorAuditLog;
import com.manacommunity.api.visitor.service.VisitorPassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorPassController {

    private final VisitorPassService visitorPassService;
    private final LoggedInUserService loggedInUserService;
    private final PiiMaskingService piiMaskingService;

    /**
     * Masks the visitorPhone in all responses for security guard callers.
     * Guards have 'Manage Gate' permission but should not see full phone numbers.
     */
    private List<VisitorPassResponse> maskPhoneForGuard(List<VisitorPassResponse> passes) {
        return passes.stream().map(p -> {
            if (p.getVisitorPhone() != null) {
                return VisitorPassResponse.builder()
                        .id(p.getId())
                        .passCode(p.getPassCode())
                        .visitorName(p.getVisitorName())
                        .visitorPhone(piiMaskingService.maskPhone(p.getVisitorPhone()))
                        .vehicleNumber(p.getVehicleNumber())
                        .purpose(p.getPurpose())
                        .passType(p.getPassType())
                        .status(p.getStatus())
                        .expectedAt(p.getExpectedAt())
                        .checkedInAt(p.getCheckedInAt())
                        .checkedOutAt(p.getCheckedOutAt())
                        .flatNumber(p.getFlatNumber())
                        .residentId(p.getResidentId())
                        .residentName(p.getResidentName())
                        .communityId(p.getCommunityId())
                        .createdAt(p.getCreatedAt())
                        .otpExpiresAt(p.getOtpExpiresAt())
                        .gateIn(p.getGateIn())
                        .gateOut(p.getGateOut())
                        .guardIn(p.getGuardIn())
                        .guardOut(p.getGuardOut())
                        .visitorPhoto(p.getVisitorPhoto())
                        .build();
            }
            return p;
        }).collect(Collectors.toList());
    }

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
        // Mask visitor phone for security guards who should not see full numbers
        return ResponseEntity.ok(maskPhoneForGuard(visitorPassService.getActivePasses(communityId)));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<List<VisitorPassResponse>> getTodaysPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        // Mask visitor phone for security guards who should not see full numbers
        return ResponseEntity.ok(maskPhoneForGuard(visitorPassService.getTodaysPasses(communityId)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Visitors')")
    public ResponseEntity<List<VisitorPassResponse>> getMyPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.getMyPasses(user.getId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('View Visitors')")
    public ResponseEntity<List<VisitorPassResponse>> getPendingApprovals(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.getPendingApprovals(user.getId()));
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

    @PostMapping("/walk-in")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> createWalkIn(
            @Valid @RequestBody VisitorPassRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(visitorPassService.createWalkIn(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('Create Visitor Pass')")
    public ResponseEntity<VisitorPassResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.approvePass(id, user));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> checkIn(
            @PathVariable Long id,
            @RequestParam(value = "gate", required = false) String gate,
            @RequestParam(value = "guard", required = false) String guard,
            @RequestBody(required = false) Map<String, String> body) {
        String photo = body != null ? body.get("visitorPhoto") : null;
        return ResponseEntity.ok(visitorPassService.checkIn(id, gate, guard, photo));
    }

    @PutMapping("/code/{passCode}/check-in")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> checkInByCode(@PathVariable String passCode) {
        return ResponseEntity.ok(visitorPassService.checkInByCode(passCode));
    }

    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> checkOut(
            @PathVariable Long id,
            @RequestParam(value = "gate", required = false) String gate,
            @RequestParam(value = "guard", required = false) String guard) {
        return ResponseEntity.ok(visitorPassService.checkOut(id, gate, guard));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('Create Visitor Pass', 'Manage Gate')")
    public ResponseEntity<VisitorPassResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(visitorPassService.rejectPass(id, user.getFullName()));
    }

    @GetMapping("/verify")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<VisitorPassResponse> verifyPass(
            @RequestParam("query") String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(visitorPassService.verifyPassCodeOrOtpOrPhone(query, communityId));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('View Visitors', 'Manage Gate')")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(visitorPassService.getAnalytics(communityId));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyAuthority('View Visitors', 'Manage Gate')")
    public ResponseEntity<List<VisitorAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(visitorPassService.getRecentAuditLogs());
    }
}
