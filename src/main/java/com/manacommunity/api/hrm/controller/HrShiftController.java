package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.HrEmployeeShift;
import com.manacommunity.api.hrm.entity.HrOrganization;
import com.manacommunity.api.hrm.entity.HrShift;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrOrganizationService;
import com.manacommunity.api.hrm.service.HrShiftService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/organizations/{orgId}/shifts")
@RequiredArgsConstructor
public class HrShiftController {

    private final HrShiftService shiftService;
    private final HrOrganizationService organizationService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrShift> createShift(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateShiftRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        HrOrganization org = organizationService.findByIdAndCommunityId(orgId, communityId);
        return ResponseEntity.ok(shiftService.createShift(org, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrShift>> listShifts(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(shiftService.listShifts(orgId));
    }

    @PostMapping("/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrEmployeeShift> assignShift(
            @PathVariable Long orgId,
            @Valid @RequestBody AssignShiftRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(shiftService.assignShift(orgId, request));
    }

    @GetMapping("/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrEmployeeShift>> getSchedule(
            @PathVariable Long orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(shiftService.getOrgShiftsForDate(orgId, date, PageRequest.of(page, size)));
    }
}
