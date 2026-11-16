package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrOnboardingTask;
import com.manacommunity.api.hrm.service.HrOnboardingService;
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
@RequestMapping("/api/v1/hr/organizations/{orgId}/onboarding")
@RequiredArgsConstructor
public class HrOnboardingController {

    private final HrOnboardingService onboardingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/employees/{employeeId}/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrOnboardingTask>> getEmployeeTasks(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(onboardingService.getEmployeeTasks(employeeId));
    }

    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrOnboardingTask> completeTask(
            @PathVariable Long orgId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(onboardingService.completeTask(taskId, user.getId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrOnboardingTask>> getPendingTasks(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(onboardingService.getPendingTasks(orgId));
    }
}
