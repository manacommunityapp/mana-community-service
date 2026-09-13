package com.manacommunity.api.controller;

import com.manacommunity.api.dto.karate.*;
import com.manacommunity.api.model.karate.*;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsKarateService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.permissions.SportsPermissions.*;

@RestController
@RequestMapping("/api/sports/karate")
@RequiredArgsConstructor
public class SportsKarateController {

    private final SportsKarateService karateService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    // ══════════════════════════════════════════════════════════════════════
    // Belts
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/belts")
    public ResponseEntity<List<SportsKarateBelt>> getBelts(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.getBelts(caller));
    }

    @PostMapping("/belts")
    public ResponseEntity<SportsKarateBelt> createBelt(
            @Valid @RequestBody KarateBeltRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.createBelt(req, caller));
    }

    @PutMapping("/belts/{id}")
    public ResponseEntity<SportsKarateBelt> updateBelt(
            @PathVariable Long id,
            @Valid @RequestBody KarateBeltRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.updateBelt(id, req));
    }

    @DeleteMapping("/belts/{id}")
    public ResponseEntity<Void> deleteBelt(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN);
        karateService.deleteBelt(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Programs
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/programs")
    public ResponseEntity<SportsKarateProgram> createProgram(
            @Valid @RequestBody KarateProgramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.createProgram(req, caller));
    }

    @GetMapping("/programs")
    public ResponseEntity<Page<SportsKarateProgram>> getPrograms(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.getPrograms(caller, pageable));
    }

    @GetMapping("/programs/{id}")
    public ResponseEntity<SportsKarateProgram> getProgram(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getProgram(id));
    }

    @PutMapping("/programs/{id}")
    public ResponseEntity<SportsKarateProgram> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody KarateProgramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.updateProgram(id, req));
    }

    @DeleteMapping("/programs/{id}")
    public ResponseEntity<Void> deleteProgram(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN);
        karateService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Batches
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/programs/{programId}/batches")
    public ResponseEntity<SportsKarateBatch> createBatch(
            @PathVariable Long programId,
            @Valid @RequestBody KarateBatchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.createBatch(programId, req, caller));
    }

    @GetMapping("/programs/{programId}/batches")
    public ResponseEntity<List<SportsKarateBatch>> getBatches(
            @PathVariable Long programId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getBatches(programId));
    }

    @PutMapping("/batches/{id}")
    public ResponseEntity<SportsKarateBatch> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody KarateBatchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.updateBatch(id, req));
    }

    @PutMapping("/batches/{id}/status")
    public ResponseEntity<SportsKarateBatch> updateBatchStatus(
            @PathVariable Long id,
            @RequestParam SportsKarateBatch.BatchStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.updateBatchStatus(id, status, caller));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Enrollments
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/batches/{batchId}/enroll")
    public ResponseEntity<SportsKarateEnrollment> enroll(
            @PathVariable Long batchId,
            @Valid @RequestBody KarateEnrollRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN, CREATE_EDIT_EVENT_REGISTRATIONS);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.enroll(batchId, req, caller));
    }

    @GetMapping("/batches/{batchId}/enrollments")
    public ResponseEntity<List<SportsKarateEnrollment>> getEnrollments(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_EVENT_REGISTRATIONS);
        return ResponseEntity.ok(karateService.getEnrollments(batchId));
    }

    @GetMapping("/enrollments/mine")
    public ResponseEntity<List<SportsKarateEnrollment>> getMyEnrollments(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.getMyEnrollments(caller));
    }

    @PutMapping("/enrollments/{id}/status")
    public ResponseEntity<SportsKarateEnrollment> updateEnrollmentStatus(
            @PathVariable Long id,
            @RequestParam SportsKarateEnrollment.EnrollmentStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.updateEnrollmentStatus(id, status));
    }

    @GetMapping("/enrollments/{id}/progress")
    public ResponseEntity<SportsKarateEnrollment> getEnrollmentProgress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getEnrollmentProgress(id));
    }

    @GetMapping("/batches/{batchId}/grading-eligible")
    public ResponseEntity<List<SportsKarateEnrollment>> getGradingEligible(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.getGradingEligible(batchId));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Class Sessions
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/batches/{batchId}/classes/generate")
    public ResponseEntity<List<SportsKarateClass>> generateClasses(
            @PathVariable Long batchId,
            @Valid @RequestBody KarateClassGenerateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.generateClasses(batchId, req, caller));
    }

    @GetMapping("/batches/{batchId}/classes")
    public ResponseEntity<List<SportsKarateClass>> getClasses(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getClasses(batchId));
    }

    @GetMapping("/classes/{id}")
    public ResponseEntity<SportsKarateClass> getClass(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getClass(id));
    }

    @PutMapping("/classes/{id}")
    public ResponseEntity<SportsKarateClass> updateClass(
            @PathVariable Long id,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String classNotes,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.updateClass(id, topic, classNotes));
    }

    @PutMapping("/classes/{id}/cancel")
    public ResponseEntity<SportsKarateClass> cancelClass(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.cancelClass(id, reason, caller));
    }

    @PutMapping("/classes/{id}/start")
    public ResponseEntity<SportsKarateClass> startClass(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.startClass(id, caller));
    }

    @PutMapping("/classes/{id}/complete")
    public ResponseEntity<SportsKarateClass> completeClass(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.completeClass(id, caller));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Attendance
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/classes/{classId}/attendance")
    public ResponseEntity<List<SportsKarateAttendance>> markAttendance(
            @PathVariable Long classId,
            @Valid @RequestBody List<KarateAttendanceEntry> entries,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.markAttendance(classId, entries, caller));
    }

    @GetMapping("/classes/{classId}/attendance")
    public ResponseEntity<List<SportsKarateAttendance>> getAttendanceForClass(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_EVENT_REGISTRATIONS);
        return ResponseEntity.ok(karateService.getAttendanceForClass(classId));
    }

    @GetMapping("/enrollments/{enrollmentId}/attendance")
    public ResponseEntity<List<SportsKarateAttendance>> getAttendanceForEnrollment(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getAttendanceForEnrollment(enrollmentId));
    }

    @PutMapping("/attendance/{id}")
    public ResponseEntity<SportsKarateAttendance> updateAttendance(
            @PathVariable Long id,
            @RequestParam SportsKarateAttendance.AttendanceStatus status,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.updateAttendance(id, status, notes));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Grading Exams
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/batches/{batchId}/exams")
    public ResponseEntity<SportsKarateGradingExam> scheduleExam(
            @PathVariable Long batchId,
            @Valid @RequestBody KarateGradingExamRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.scheduleExam(batchId, req, caller));
    }

    @GetMapping("/batches/{batchId}/exams")
    public ResponseEntity<List<SportsKarateGradingExam>> getExams(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.getExams(batchId));
    }

    @GetMapping("/exams/{examId}/eligible")
    public ResponseEntity<List<SportsKarateEnrollment>> getEligibleForExam(
            @PathVariable Long examId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.getEligibleForExam(examId));
    }

    @PostMapping("/exams/{examId}/results")
    public ResponseEntity<List<SportsKarateExamResult>> submitResults(
            @PathVariable Long examId,
            @Valid @RequestBody List<KarateExamResultEntry> entries,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.submitResults(examId, entries, caller));
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<List<SportsKarateExamResult>> getExamResults(
            @PathVariable Long examId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(karateService.getExamResults(examId));
    }

    @PutMapping("/exams/{id}/cancel")
    public ResponseEntity<SportsKarateGradingExam> cancelExam(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.cancelExam(id, caller));
    }
}
