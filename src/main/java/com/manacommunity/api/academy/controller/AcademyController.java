package com.manacommunity.api.academy.controller;

import com.manacommunity.api.academy.entity.AcademyEnrollment;
import com.manacommunity.api.academy.entity.AcademyProgram;
import com.manacommunity.api.academy.repository.AcademyEnrollmentRepository;
import com.manacommunity.api.academy.repository.AcademyProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/academy")
@RequiredArgsConstructor
public class AcademyController {

    private final AcademyProgramRepository programRepo;
    private final AcademyEnrollmentRepository enrollmentRepo;

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(List.of(
                "SPORTS", "ARTS", "TECHNOLOGY", "LANGUAGE", "FITNESS", "ACADEMIC", "MUSIC", "COOKING"));
    }

    @GetMapping("/programs")
    public ResponseEntity<List<AcademyProgram>> getPrograms(
            @RequestParam Long communityId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(programRepo.findByCommunityIdAndCategoryOrderByCreatedAtDesc(communityId, category));
        }
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(programRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status));
        }
        return ResponseEntity.ok(programRepo.findByCommunityIdOrderByCreatedAtDesc(communityId));
    }

    @GetMapping("/programs/{id}")
    public ResponseEntity<AcademyProgram> getProgram(@PathVariable Long id) {
        return programRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/programs/{programId}/enroll")
    @Transactional
    public ResponseEntity<AcademyEnrollment> enroll(
            @PathVariable Long programId,
            @RequestParam Long userId) {
        return programRepo.findById(programId).map(p -> {
            AcademyEnrollment enrollment = enrollmentRepo.findByProgramIdAndUserId(programId, userId)
                    .orElseGet(() -> {
                        p.setEnrolledCount(p.getEnrolledCount() + 1);
                        programRepo.save(p);
                        return enrollmentRepo.save(AcademyEnrollment.builder()
                                .programId(programId)
                                .userId(userId)
                                .build());
                    });
            return ResponseEntity.ok(enrollment);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-enrollments")
    public ResponseEntity<List<AcademyEnrollment>> myEnrollments(@RequestParam Long userId) {
        return ResponseEntity.ok(enrollmentRepo.findByUserIdOrderByEnrolledAtDesc(userId));
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<Map<String, Object>>> getCertificates(@RequestParam Long userId) {
        List<AcademyEnrollment> completed = enrollmentRepo.findByUserIdOrderByEnrolledAtDesc(userId)
                .stream().filter(e -> "COMPLETED".equals(e.getStatus())).toList();
        List<Map<String, Object>> certs = completed.stream().map(e -> Map.<String, Object>of(
                "id", "CERT-" + e.getId(),
                "enrollmentId", e.getId(),
                "programId", e.getProgramId(),
                "issuedAt", e.getEnrolledAt() != null ? e.getEnrolledAt().toLocalDate().toString() : ""
        )).toList();
        return ResponseEntity.ok(certs);
    }

    @GetMapping("/certificates/{certId}")
    public ResponseEntity<Map<String, Object>> getCertificate(@PathVariable String certId) {
        return ResponseEntity.ok(Map.of("id", certId, "status", "ISSUED"));
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "checkedInAt", java.time.LocalDateTime.now().toString()));
    }

    @GetMapping("/instructors")
    public ResponseEntity<List<Map<String, Object>>> getInstructors(@RequestParam Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/instructors/apply")
    public ResponseEntity<Map<String, Object>> applyAsInstructor(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "message", "Application submitted for review."));
    }

    @GetMapping("/teaching/dashboard")
    public ResponseEntity<Map<String, Object>> teachingDashboard(@RequestParam Long userId) {
        return ResponseEntity.ok(Map.of(
                "activeWorkshops", 0,
                "totalStudents", 0,
                "upcomingSessions", List.of()
        ));
    }

    @PostMapping("/teaching/workshops")
    public ResponseEntity<AcademyProgram> createWorkshop(@RequestBody AcademyProgram program) {
        return ResponseEntity.ok(programRepo.save(program));
    }

    @PostMapping("/teaching/attendees/{attendeeId}/toggle-attendance")
    public ResponseEntity<Map<String, Object>> toggleAttendance(@PathVariable Long attendeeId) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> adminStats(@RequestParam Long communityId) {
        return ResponseEntity.ok(Map.of(
                "totalPrograms", programRepo.count(),
                "activePrograms", programRepo.countByCommunityIdAndStatus(communityId, "OPEN"),
                "totalEnrollments", enrollmentRepo.count(),
                "pendingInstructors", 0
        ));
    }

    @GetMapping("/admin/instructors/pending")
    public ResponseEntity<List<Object>> pendingInstructors() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/admin/instructors/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveInstructor(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/admin/instructors/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectInstructor(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/admin/programs/pending")
    public ResponseEntity<List<AcademyProgram>> pendingPrograms(@RequestParam Long communityId) {
        return ResponseEntity.ok(programRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, "PENDING_APPROVAL"));
    }

    @PostMapping("/admin/programs/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveProgram(@PathVariable Long id) {
        programRepo.findById(id).ifPresent(p -> { p.setStatus("OPEN"); programRepo.save(p); });
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/admin/programs/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectProgram(@PathVariable Long id) {
        programRepo.findById(id).ifPresent(p -> { p.setStatus("CLOSED"); programRepo.save(p); });
        return ResponseEntity.ok(Map.of("success", true));
    }
}
