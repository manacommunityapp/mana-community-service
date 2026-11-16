package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrOffboardingTask;
import com.manacommunity.api.hrm.service.HrOffboardingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/organizations/{orgId}/offboarding")
@RequiredArgsConstructor
public class HrOffboardingController {

    private final HrOffboardingService offboardingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/employees/{employeeId}/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrOffboardingTask>> getEmployeeTasks(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offboardingService.getEmployeeTasks(employeeId));
    }

    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrOffboardingTask> completeTask(
            @PathVariable Long orgId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offboardingService.completeTask(taskId, user.getId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrOffboardingTask>> getPendingTasks(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offboardingService.getPendingTasks(orgId));
    }
}
