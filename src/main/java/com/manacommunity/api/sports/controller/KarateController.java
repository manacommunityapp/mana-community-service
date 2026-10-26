package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.service.KarateService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.permissions.SportsPermissions.*;

/**
 * REST controller for the Karate Classes sub-module.
 *
 * Base path: /api/sports/karate
 *
 * Endpoints:
 *   GET  /belts                            → list 9 belt levels
 *   POST /programs                         → create program (admin)
 *   GET  /programs                         → list programs (member+)
 *   GET  /programs/{id}                    → get single program
 *   POST /programs/{id}/batches            → create batch (admin)
 *   GET  /programs/{id}/batches            → list batches (member+)
 *   PUT  /batches/{id}/status              → update batch status (admin)
 *   POST /batches/{id}/enroll              → enroll self (member)
 *   GET  /batches/{id}/attendance          → attendance summary (admin)
 *   POST /batches/{id}/exams               → create grading exam (admin)
 *   GET  /batches/{id}/exams               → list exams for batch
 *   POST /exams/{id}/results               → record/upsert exam result (admin)
 *   GET  /exams/{id}/results               → list results for exam (admin)
 */
@Slf4j
@RestController
@RequestMapping("/api/sports/karate")
@RequiredArgsConstructor
public class KarateController {

    private final KarateService karateService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    // ── Belts ────────────────────────────────────────────────────────────────

    @GetMapping("/belts")
    public ResponseEntity<List<KarateBelt>> listBelts(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        return ResponseEntity.ok(karateService.listBelts());
    }

    // ── Programs ─────────────────────────────────────────────────────────────

    @PostMapping("/programs")
    public ResponseEntity<KarateProgram> createProgram(
            @Valid @RequestBody KarateProgramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        log.info("POST /api/sports/karate/programs name={}", req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.createProgram(req, admin));
    }

    @GetMapping("/programs")
    public ResponseEntity<List<KarateProgram>> listPrograms(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.listPrograms(user));
    }

    @GetMapping("/programs/{id}")
    public ResponseEntity<KarateProgram> getProgram(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.getProgram(id, user));
    }

    // ── Batches ───────────────────────────────────────────────────────────────

    @PostMapping("/programs/{id}/batches")
    public ResponseEntity<KarateBatch> createBatch(
            @PathVariable Long id,
            @Valid @RequestBody KarateBatchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        log.info("POST /api/sports/karate/programs/{}/batches name={}", id, req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.createBatch(id, req, admin));
    }

    @GetMapping("/programs/{id}/batches")
    public ResponseEntity<List<KarateBatch>> listBatches(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.listBatches(id, user));
    }

    @PutMapping("/batches/{id}/status")
    public ResponseEntity<KarateBatch> updateBatchStatus(
            @PathVariable Long id,
            @Valid @RequestBody KarateBatchStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        log.info("PUT /api/sports/karate/batches/{}/status status={}", id, req.getStatus());
        return ResponseEntity.ok(karateService.updateBatchStatus(id, req.getStatus(), admin));
    }

    // ── Enrollment ────────────────────────────────────────────────────────────

    @PostMapping("/batches/{id}/enroll")
    public ResponseEntity<KarateEnrollment> enroll(
            @PathVariable Long id,
            @RequestBody(required = false) KarateEnrollRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        log.info("POST /api/sports/karate/batches/{}/enroll userId={}", id, user.getId());
        KarateEnrollRequest enrollReq = req != null ? req : new KarateEnrollRequest();
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.enroll(id, enrollReq, user));
    }

    @GetMapping("/batches/{id}/attendance")
    public ResponseEntity<KarateAttendanceResponse> getAttendance(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.getAttendance(id, admin));
    }

    // ── Exams ─────────────────────────────────────────────────────────────────

    @PostMapping("/batches/{id}/exams")
    public ResponseEntity<KarateExam> createExam(
            @PathVariable Long id,
            @Valid @RequestBody KarateExamRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        log.info("POST /api/sports/karate/batches/{}/exams title={}", id, req.getExamTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.createExam(id, req, admin));
    }

    @GetMapping("/batches/{id}/exams")
    public ResponseEntity<List<KarateExam>> listExams(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.listExams(id, user));
    }

    @PostMapping("/exams/{id}/results")
    public ResponseEntity<KarateExamResult> recordResult(
            @PathVariable Long id,
            @Valid @RequestBody KarateExamResultRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        log.info("POST /api/sports/karate/exams/{}/results userId={} result={}", id, req.getUserId(), req.getResult());
        return ResponseEntity.status(HttpStatus.CREATED).body(karateService.recordResult(id, req, admin));
    }

    @GetMapping("/exams/{id}/results")
    public ResponseEntity<List<KarateExamResult>> listResults(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        AppUser admin = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(karateService.listResults(id, admin));
    }
}
