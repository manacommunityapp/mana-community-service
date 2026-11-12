package com.manacommunity.api.mw.controller;

import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.mw.dto.CreateHandoverRequest;
import com.manacommunity.api.mw.service.MwHandoverService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/mw/handover")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MwHandoverController {

    private final MwHandoverService handoverService;
    private final LoggedInUserService loggedInUserService;
    private final HrEmployeeService employeeService;

    @PostMapping("/orgs/{orgId}/handovers")
    public ResponseEntity<?> createHandover(
            @PathVariable Long orgId,
            @RequestBody CreateHandoverRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.verifyManagerAccess(orgId, user.getId());
        return ResponseEntity.ok(handoverService.createHandover(orgId, req));
    }

    @PutMapping("/orgs/{orgId}/handovers/{handoverId}/acknowledge")
    public ResponseEntity<?> acknowledgeHandover(
            @PathVariable Long orgId,
            @PathVariable Long handoverId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(handoverService.acknowledgeHandover(orgId, handoverId));
    }

    @GetMapping("/orgs/{orgId}/handovers")
    public ResponseEntity<?> getHandovers(
            @PathVariable Long orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(handoverService.getHandovers(orgId, date));
    }

    @GetMapping("/orgs/{orgId}/handovers/pending")
    public ResponseEntity<?> getPendingAcknowledgements(
            @PathVariable Long orgId,
            @RequestParam Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(handoverService.getPendingAcknowledgements(orgId, employeeId));
    }
}
