package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.HrEmployeeDocument;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/organizations/{orgId}/employees")
@RequiredArgsConstructor
public class HrEmployeeController {

    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(employeeService.createEmployee(orgId, communityId, request, user.getId()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EmployeeListResponse>> listEmployees(
            @PathVariable Long orgId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(employeeService.listEmployees(orgId, status,
                PageRequest.of(page, size, Sort.by("firstName").ascending())));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> getEmployee(
            @PathVariable Long orgId,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getEmployee(orgId, employeeId));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(employeeService.updateEmployee(orgId, employeeId, request, user.getId()));
    }

    @PostMapping("/{employeeId}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> uploadDocument(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.uploadDocument(orgId, employeeId, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{employeeId}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrEmployeeDocument>> listDocuments(
            @PathVariable Long orgId,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.listDocuments(employeeId));
    }
}
