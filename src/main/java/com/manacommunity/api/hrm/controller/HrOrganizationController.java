package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrOrganizationService;
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
@RequestMapping("/api/v1/hr/organizations")
@RequiredArgsConstructor
public class HrOrganizationController {

    private final HrOrganizationService organizationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.createOrganization(communityId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationResponse> getOrganization(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.getOrganization(id, communityId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationResponse>> listOrganizations(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.listOrganizations(communityId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.updateOrganization(id, communityId, request));
    }

    @PostMapping("/{id}/branches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrBranch> addBranch(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.addBranch(id, communityId, request));
    }

    @GetMapping("/{id}/branches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrBranch>> listBranches(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.listBranches(id));
    }

    @PostMapping("/{id}/departments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrDepartment> addDepartment(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepartmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.addDepartment(id, communityId, request));
    }

    @GetMapping("/{id}/departments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrDepartment>> listDepartments(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.listDepartments(id));
    }

    @PostMapping("/{id}/designations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrDesignation> addDesignation(
            @PathVariable Long id,
            @Valid @RequestBody CreateDesignationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(organizationService.addDesignation(id, communityId, request));
    }

    @GetMapping("/{id}/designations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrDesignation>> listDesignations(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.listDesignations(id));
    }
}
