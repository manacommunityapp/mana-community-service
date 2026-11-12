package com.manacommunity.api.mw.controller;

import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.mw.dto.CompleteFreshDeliveryRequest;
import com.manacommunity.api.mw.dto.CreateFreshDeliveryRequest;
import com.manacommunity.api.mw.entity.MwFreshAssignment;
import com.manacommunity.api.mw.service.MwFreshIntegrationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mw/integrations/fresh")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MwFreshIntegrationController {

    private final MwFreshIntegrationService freshService;
    private final LoggedInUserService loggedInUserService;
    private final HrEmployeeService employeeService;

    @PostMapping("/{orgId}/deliveries")
    public ResponseEntity<MwFreshAssignment> createDelivery(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateFreshDeliveryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(freshService.createDeliveryAssignment(orgId, req));
    }

    @PutMapping("/{orgId}/deliveries/{id}/complete")
    public ResponseEntity<MwFreshAssignment> completeDelivery(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @RequestBody CompleteFreshDeliveryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(freshService.completeDeliveryAssignment(orgId, id, req));
    }

    @GetMapping("/{orgId}/deliveries")
    public ResponseEntity<List<MwFreshAssignment>> getDeliveries(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(freshService.getOrgDeliveries(orgId));
    }

    @GetMapping("/{orgId}/employees/{employeeId}/deliveries")
    public ResponseEntity<List<MwFreshAssignment>> getEmployeeDeliveries(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(freshService.getEmployeeDeliveries(orgId, employeeId));
    }
}
