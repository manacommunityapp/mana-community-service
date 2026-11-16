package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrPerformanceService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/performance")
@RequiredArgsConstructor
public class HrPerformanceController {
    private final HrPerformanceService performanceService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    // Appraisal Cycles
    @PostMapping("/organizations/{orgId}/cycles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAppraisalCycle> createCycle(@PathVariable Long orgId,
            @RequestBody HrAppraisalCycle cycle, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        cycle.setCreatedBy(user.getId());
        return ResponseEntity.ok(performanceService.createCycle(orgId, cycle));
    }

    @GetMapping("/organizations/{orgId}/cycles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrAppraisalCycle>> getCycles(@PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getCycles(orgId));
    }

    @PatchMapping("/organizations/{orgId}/cycles/{cycleId}/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAppraisalCycle> activateCycle(@PathVariable Long orgId, @PathVariable Long cycleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.activateCycle(orgId, cycleId));
    }

    // Goals
    @PostMapping("/organizations/{orgId}/employees/{employeeId}/goals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrGoal> createGoal(@PathVariable Long orgId, @PathVariable Long employeeId,
            @RequestBody HrGoal goal, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.createGoal(orgId, employeeId, goal));
    }

    @GetMapping("/employees/{employeeId}/goals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrGoal>> getGoals(@PathVariable Long employeeId,
            @RequestParam Long orgId,
            @RequestParam(required = false) Long cycleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getEmployeeGoals(employeeId, cycleId));
    }

    @PatchMapping("/goals/{goalId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrGoal> updateGoalProgress(@PathVariable Long goalId,
            @RequestParam Long orgId,
            @RequestParam Integer achievementPercent,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.updateGoalProgress(goalId, achievementPercent));
    }

    // Appraisal Forms
    @PostMapping("/employees/{employeeId}/self-appraisal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAppraisalForm> submitSelfAppraisal(@PathVariable Long employeeId,
            @RequestParam Long orgId, @RequestParam Long cycleId,
            @RequestParam Integer rating, @RequestParam(required = false) String comments,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.submitSelfAppraisal(orgId, employeeId, cycleId, rating, comments));
    }

    @PostMapping("/forms/{formId}/manager-review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrAppraisalForm> submitManagerReview(@PathVariable Long formId,
            @RequestParam Long orgId, @RequestParam Integer rating,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee reviewer = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(performanceService.submitManagerAppraisal(formId, rating, comments, reviewer.getId()));
    }

    @GetMapping("/organizations/{orgId}/cycles/{cycleId}/forms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrAppraisalForm>> getCycleForms(@PathVariable Long orgId, @PathVariable Long cycleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getCycleForms(cycleId));
    }

    @GetMapping("/my-pending-reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrAppraisalForm>> getPendingReviews(@RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee reviewer = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getPendingReviews(reviewer.getId()));
    }

    // OKRs
    @PostMapping("/organizations/{orgId}/okrs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrOkr> createOkr(@PathVariable Long orgId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        HrOkr okr = new HrOkr();
        if (body.containsKey("objective")) okr.setObjective((String) body.get("objective"));
        if (body.containsKey("quarter")) okr.setQuarter((String) body.get("quarter"));
        if (body.containsKey("year")) okr.setYear((Integer) body.get("year"));
        Long employeeId = body.containsKey("employeeId") ? ((Number) body.get("employeeId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<HrOkrKeyResult> keyResults = (List<HrOkrKeyResult>) body.getOrDefault("keyResults", List.of());
        return ResponseEntity.ok(performanceService.createOkr(orgId, employeeId, okr, keyResults));
    }

    @GetMapping("/employees/{employeeId}/okrs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrOkr>> getEmployeeOkrs(@PathVariable Long employeeId,
            @RequestParam Long orgId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getEmployeeOkrs(employeeId));
    }

    @GetMapping("/organizations/{orgId}/okrs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrOkr>> getOrgOkrs(@PathVariable Long orgId,
            @RequestParam(required = false) Integer year, @RequestParam(required = false) String quarter,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getOrgOkrs(orgId, year, quarter));
    }

    @PatchMapping("/key-results/{keyResultId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrOkrKeyResult> updateKeyResult(@PathVariable Long keyResultId,
            @RequestParam Long orgId, @RequestParam Integer progressPercent,
            @RequestParam(required = false) String currentValue,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.updateKeyResult(keyResultId, progressPercent, currentValue));
    }

    // 360 Feedback
    @PostMapping("/organizations/{orgId}/feedback-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrFeedbackRequest> createFeedbackRequest(@PathVariable Long orgId,
            @RequestParam Long subjectEmployeeId,
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            @RequestParam(defaultValue = "true") Boolean isAnonymous,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.createFeedbackRequest(orgId, subjectEmployeeId, cycleId, deadline, isAnonymous, user.getId()));
    }

    @PostMapping("/feedback-requests/{requestId}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrFeedbackResponse> submitFeedback(@PathVariable Long requestId,
            @RequestParam Long orgId, @RequestBody HrFeedbackResponse response,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee reviewer = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(performanceService.submitFeedback(requestId, reviewer.getId(), response));
    }

    @GetMapping("/feedback-requests/{requestId}/responses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrFeedbackResponse>> getFeedbackResponses(@PathVariable Long requestId,
            @RequestParam Long orgId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(performanceService.getFeedbackResponses(requestId));
    }
}
