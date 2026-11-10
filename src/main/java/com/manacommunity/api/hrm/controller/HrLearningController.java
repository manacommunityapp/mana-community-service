package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrLearningService;
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
@RequestMapping("/api/v1/hr/learning")
@RequiredArgsConstructor
public class HrLearningController {
    private final HrLearningService learningService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    // Training Programs
    @PostMapping("/organizations/{orgId}/programs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTrainingProgram> createProgram(@PathVariable Long orgId,
            @RequestBody HrTrainingProgram program, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        program.setCreatedBy(user.getId());
        return ResponseEntity.ok(learningService.createProgram(orgId, program));
    }

    @GetMapping("/organizations/{orgId}/programs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrTrainingProgram>> getPrograms(@PathVariable Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.getPrograms(orgId, status, PageRequest.of(page, size)));
    }

    @PostMapping("/organizations/{orgId}/programs/{programId}/enroll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTrainingEnrollment> enroll(@PathVariable Long orgId, @PathVariable Long programId,
            @RequestParam Long employeeId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.enroll(orgId, employeeId, programId));
    }

    @PatchMapping("/enrollments/{enrollmentId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTrainingEnrollment> completeEnrollment(@PathVariable Long enrollmentId,
            @RequestParam Long orgId,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) String feedback,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.completeEnrollment(enrollmentId, score, feedback));
    }

    @GetMapping("/employees/{employeeId}/enrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrTrainingEnrollment>> getEnrollments(@PathVariable Long employeeId,
            @RequestParam Long orgId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.getEmployeeEnrollments(employeeId));
    }

    // Certifications
    @PostMapping("/organizations/{orgId}/employees/{employeeId}/certifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrCertification> addCertification(@PathVariable Long orgId, @PathVariable Long employeeId,
            @RequestBody HrCertification cert, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.addCertification(orgId, employeeId, cert));
    }

    @GetMapping("/employees/{employeeId}/certifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrCertification>> getCertifications(@PathVariable Long employeeId,
            @RequestParam Long orgId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.getEmployeeCertifications(employeeId));
    }

    // Career Path
    @PostMapping("/organizations/{orgId}/employees/{employeeId}/career-path")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrCareerPath> saveCareerPath(@PathVariable Long orgId, @PathVariable Long employeeId,
            @RequestBody HrCareerPath path, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.saveCareerPath(orgId, employeeId, path));
    }

    @GetMapping("/employees/{employeeId}/career-path")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrCareerPath> getCareerPath(@PathVariable Long employeeId,
            @RequestParam Long orgId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.getActiveCareerPath(employeeId));
    }

    // Recognition
    @PostMapping("/organizations/{orgId}/recognitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrRecognition> giveRecognition(@PathVariable Long orgId,
            @RequestBody Map<String, Object> body, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee giver = employeeService.findByOrgAndManaUser(orgId, user.getId());
        Long receiverEmployeeId = ((Number) body.get("receiverEmployeeId")).longValue();
        String type = (String) body.getOrDefault("type", "KUDOS");
        String message = (String) body.get("message");
        Integer points = body.containsKey("points") ? ((Number) body.get("points")).intValue() : 0;
        Boolean isPublic = body.containsKey("isPublic") ? (Boolean) body.get("isPublic") : true;
        String category = (String) body.get("category");
        return ResponseEntity.ok(learningService.giveRecognition(orgId, giver.getId(), receiverEmployeeId,
            type, message, points, isPublic, category));
    }

    @GetMapping("/organizations/{orgId}/recognitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrRecognition>> getPublicRecognitions(@PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.getPublicRecognitions(orgId, PageRequest.of(page, size)));
    }

    @GetMapping("/employees/{employeeId}/recognitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrRecognition>> getMyRecognitions(@PathVariable Long employeeId,
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(learningService.getMyRecognitions(employeeId, PageRequest.of(page, size)));
    }
}
