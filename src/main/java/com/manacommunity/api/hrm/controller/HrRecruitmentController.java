package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrRecruitmentService;
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

@RestController
@RequestMapping("/api/v1/hr/recruitment")
@RequiredArgsConstructor
public class HrRecruitmentController {
    private final HrRecruitmentService recruitmentService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/organizations/{orgId}/postings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrJobPosting> createPosting(@PathVariable Long orgId, @RequestBody HrJobPosting posting,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        posting.setCreatedBy(user.getId());
        return ResponseEntity.ok(recruitmentService.createJobPosting(orgId, posting));
    }

    @GetMapping("/organizations/{orgId}/postings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrJobPosting>> getPostings(@PathVariable Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.getJobPostings(orgId, status, PageRequest.of(page, size)));
    }

    @PatchMapping("/organizations/{orgId}/postings/{postingId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrJobPosting> updatePostingStatus(@PathVariable Long orgId, @PathVariable Long postingId,
            @RequestParam String status, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.updateJobPostingStatus(orgId, postingId, status));
    }

    @PostMapping("/organizations/{orgId}/postings/{postingId}/applicants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrApplicant> addApplicant(@PathVariable Long orgId, @PathVariable Long postingId,
            @RequestBody HrApplicant applicant, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.createApplicant(orgId, postingId, applicant));
    }

    @GetMapping("/organizations/{orgId}/postings/{postingId}/applicants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrApplicant>> getApplicants(@PathVariable Long orgId, @PathVariable Long postingId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.getApplicants(postingId, status, PageRequest.of(page, size)));
    }

    @PatchMapping("/organizations/{orgId}/applicants/{applicantId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrApplicant> updateApplicantStatus(@PathVariable Long orgId, @PathVariable Long applicantId,
            @RequestParam String status, @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.updateApplicantStatus(orgId, applicantId, status, notes));
    }

    @PostMapping("/organizations/{orgId}/applicants/{applicantId}/interviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrInterview> scheduleInterview(@PathVariable Long orgId, @PathVariable Long applicantId,
            @RequestBody HrInterview interview, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.scheduleInterview(orgId, applicantId, interview));
    }

    @GetMapping("/organizations/{orgId}/applicants/{applicantId}/interviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrInterview>> getInterviews(@PathVariable Long orgId, @PathVariable Long applicantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.getInterviews(applicantId));
    }

    @PostMapping("/organizations/{orgId}/applicants/{applicantId}/offers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrOffer> createOffer(@PathVariable Long orgId, @PathVariable Long applicantId,
            @RequestBody HrOffer offer, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.createOffer(orgId, applicantId, offer, user.getId()));
    }

    @PatchMapping("/organizations/{orgId}/offers/{offerId}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrOffer> respondToOffer(@PathVariable Long orgId, @PathVariable Long offerId,
            @RequestParam String response, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(recruitmentService.respondToOffer(offerId, response));
    }
}
