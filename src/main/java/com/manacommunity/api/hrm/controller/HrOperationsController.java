package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrOperationsService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/operations")
@RequiredArgsConstructor
public class HrOperationsController {

    private final HrOperationsService operationsService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    // Assets
    @PostMapping("/organizations/{orgId}/assets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAsset> createAsset(
            @PathVariable Long orgId,
            @RequestBody HrAsset asset,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.createAsset(orgId, asset));
    }

    @GetMapping("/organizations/{orgId}/assets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrAsset>> getAssets(
            @PathVariable Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.getAssets(orgId, status, PageRequest.of(page, size)));
    }

    @PostMapping("/organizations/{orgId}/assets/{assetId}/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAssetAssignment> assignAsset(
            @PathVariable Long orgId,
            @PathVariable Long assetId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee mgr = employeeService.findByOrgAndManaUser(orgId, user.getId());
        Long employeeId = ((Number) body.get("employeeId")).longValue();
        String condition = (String) body.getOrDefault("condition", null);
        return ResponseEntity.ok(operationsService.assignAsset(orgId, assetId, employeeId, mgr.getId(), condition));
    }

    @PostMapping("/organizations/{orgId}/assignments/{assignmentId}/return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAssetAssignment> returnAsset(
            @PathVariable Long orgId,
            @PathVariable Long assignmentId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        String condition = body != null ? body.get("condition") : null;
        return ResponseEntity.ok(operationsService.returnAsset(orgId, assignmentId, condition));
    }

    @GetMapping("/employees/{employeeId}/assets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrAssetAssignment>> getEmployeeAssets(
            @PathVariable Long employeeId,
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.getEmployeeAssets(employeeId));
    }

    // Tasks
    @PostMapping("/organizations/{orgId}/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTask> createTask(
            @PathVariable Long orgId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee creator = employeeService.findByOrgAndManaUser(orgId, user.getId());
        Long assignedToId = ((Number) body.get("assignedToId")).longValue();
        HrTask task = new HrTask();
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));
        if (body.get("priority") != null) task.setPriority((String) body.get("priority"));
        if (body.get("dueDate") != null) task.setDueDate(java.time.LocalDate.parse((String) body.get("dueDate")));
        return ResponseEntity.ok(operationsService.createTask(orgId, assignedToId, creator.getId(), task));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTask> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body,
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.updateTaskStatus(taskId, body.get("status")));
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrTask>> getMyTasks(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(operationsService.getMyTasks(emp.getId()));
    }

    // Announcements
    @PostMapping("/organizations/{orgId}/announcements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAnnouncement> createAnnouncement(
            @PathVariable Long orgId,
            @RequestBody HrAnnouncement announcement,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(operationsService.createAnnouncement(orgId, announcement, emp.getId()));
    }

    @GetMapping("/organizations/{orgId}/announcements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrAnnouncement>> getAnnouncements(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.getActiveAnnouncements(orgId));
    }

    // Help Desk
    @PostMapping("/organizations/{orgId}/tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrHelpDeskTicket> createTicket(
            @PathVariable Long orgId,
            @RequestBody HrHelpDeskTicket ticket,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(operationsService.createTicket(orgId, emp.getId(), ticket));
    }

    @PostMapping("/organizations/{orgId}/tickets/{ticketId}/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrHelpDeskTicket> assignTicket(
            @PathVariable Long orgId,
            @PathVariable Long ticketId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.assignTicket(orgId, ticketId, body.get("assignedToEmployeeId")));
    }

    @PostMapping("/organizations/{orgId}/tickets/{ticketId}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrHelpDeskTicket> resolveTicket(
            @PathVariable Long orgId,
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.resolveTicket(orgId, ticketId, body.get("resolution")));
    }

    @GetMapping("/my-tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrHelpDeskTicket>> getMyTickets(
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee emp = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(operationsService.getMyTickets(emp.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/organizations/{orgId}/tickets/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrHelpDeskTicket>> getOpenTickets(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(operationsService.getOpenTickets(orgId, PageRequest.of(page, size)));
    }
}
