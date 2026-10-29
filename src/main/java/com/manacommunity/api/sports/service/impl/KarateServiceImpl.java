package com.manacommunity.api.sports.service.impl;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.service.KarateService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KarateServiceImpl implements KarateService {

    private final KarateBeltRepository beltRepo;
    private final KarateProgramRepository programRepo;
    private final KarateBatchRepository batchRepo;
    private final KarateEnrollmentRepository enrollmentRepo;
    private final KarateExamRepository examRepo;
    private final KarateExamResultRepository examResultRepo;
    private final AppUserRepository userRepo;

    // ── Belts ────────────────────────────────────────────────────────────────

    @Override
    public List<KarateBelt> listBelts() {
        return beltRepo.findAllByOrderBySortOrderAsc();
    }

    // ── Programs ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public KarateProgram createProgram(KarateProgramRequest req, AppUser admin) {
        Community community = admin.getCommunity();

        KarateProgram.KarateProgramBuilder builder = KarateProgram.builder()
                .name(req.getName())
                .description(req.getDescription())
                .minAge(req.getMinAge())
                .maxAge(req.getMaxAge())
                .community(community);

        if (req.getStartingBeltId() != null) {
            builder.startingBelt(beltRepo.findById(req.getStartingBeltId())
                    .orElseThrow(() -> new ResourceNotFoundException("Belt not found: " + req.getStartingBeltId())));
        }
        if (req.getTargetBeltId() != null) {
            builder.targetBelt(beltRepo.findById(req.getTargetBeltId())
                    .orElseThrow(() -> new ResourceNotFoundException("Belt not found: " + req.getTargetBeltId())));
        }

        KarateProgram program = builder.build();
        log.info("Creating karate program name={} communityId={}", req.getName(), community.getId());
        return programRepo.save(program);
    }

    @Override
    public List<KarateProgram> listPrograms(AppUser user) {
        return programRepo.findByCommunityIdOrderByCreatedAtDesc(user.getCommunity().getId());
    }

    @Override
    public KarateProgram getProgram(Long id, AppUser user) {
        KarateProgram p = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Karate program not found: " + id));
        if (!p.getCommunity().getId().equals(user.getCommunity().getId())) {
            throw new ResourceNotFoundException("Karate program not found: " + id);
        }
        return p;
    }

    // ── Batches ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public KarateBatch createBatch(Long programId, KarateBatchRequest req, AppUser admin) {
        KarateProgram program = programRepo.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Karate program not found: " + programId));

        KarateBatch batch = KarateBatch.builder()
                .program(program)
                .name(req.getName())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .classTime(req.getClassTime())
                .daysOfWeek(req.getDaysOfWeek())
                .maxCapacity(req.getMaxCapacity())
                .venue(req.getVenue())
                .build();

        log.info("Creating karate batch name={} programId={}", req.getName(), programId);
        return batchRepo.save(batch);
    }

    @Override
    public List<KarateBatch> listBatches(Long programId, AppUser user) {
        return batchRepo.findByProgramIdOrderByStartDateAsc(programId);
    }

    @Override
    @Transactional
    public KarateBatch updateBatchStatus(Long batchId, String status, AppUser admin) {
        KarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Karate batch not found: " + batchId));
        batch.setStatus(status);
        log.info("Updating karate batch id={} status={}", batchId, status);
        return batchRepo.save(batch);
    }

    // ── Enrollment ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public KarateEnrollment enroll(Long batchId, KarateEnrollRequest req, AppUser user) {
        KarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Karate batch not found: " + batchId));

        if (!"ACTIVE".equals(batch.getStatus()) && !"UPCOMING".equals(batch.getStatus())) {
            throw new InvalidInputException("Enrollment is only open for ACTIVE or UPCOMING batches");
        }
        if (enrollmentRepo.existsByBatchIdAndUserId(batchId, user.getId())) {
            throw new InvalidInputException("Already enrolled in this batch");
        }
        if (batch.getMaxCapacity() != null && batch.getEnrolledCount() >= batch.getMaxCapacity()) {
            throw new InvalidInputException("Batch is at full capacity");
        }

        KarateEnrollment.KarateEnrollmentBuilder builder = KarateEnrollment.builder()
                .batch(batch)
                .user(user);

        if (req.getCurrentBeltId() != null) {
            builder.currentBelt(beltRepo.findById(req.getCurrentBeltId())
                    .orElseThrow(() -> new ResourceNotFoundException("Belt not found: " + req.getCurrentBeltId())));
        }

        KarateEnrollment enrollment = builder.build();
        batch.setEnrolledCount(batch.getEnrolledCount() + 1);
        batchRepo.save(batch);

        log.info("Enrolling userId={} in karate batchId={}", user.getId(), batchId);
        return enrollmentRepo.save(enrollment);
    }

    @Override
    public KarateAttendanceResponse getAttendance(Long batchId, AppUser admin) {
        KarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Karate batch not found: " + batchId));

        List<KarateEnrollment> enrollments = enrollmentRepo.findByBatchIdOrderByEnrolledAtAsc(batchId);

        List<KarateAttendanceResponse.EnrolleeStats> stats = enrollments.stream()
                .filter(e -> !"WITHDRAWN".equals(e.getStatus()))
                .map(e -> {
                    int attended = e.getClassesAttended();
                    int total = e.getTotalClasses();
                    double pct = total == 0 ? 0.0 : (attended * 100.0 / total);
                    return new KarateAttendanceResponse.EnrolleeStats(
                            e.getUser().getId(),
                            e.getUser().getFullName(),
                            attended,
                            total,
                            Math.round(pct * 10.0) / 10.0,
                            pct < 75.0 && total > 0
                    );
                })
                .collect(Collectors.toList());

        return new KarateAttendanceResponse(batchId, batch.getName(), enrollments.size(), stats);
    }

    // ── Exams ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public KarateExam createExam(Long batchId, KarateExamRequest req, AppUser admin) {
        KarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Karate batch not found: " + batchId));
        KarateBelt targetBelt = beltRepo.findById(req.getTargetBeltId())
                .orElseThrow(() -> new ResourceNotFoundException("Belt not found: " + req.getTargetBeltId()));

        KarateExam exam = KarateExam.builder()
                .batch(batch)
                .targetBelt(targetBelt)
                .examTitle(req.getExamTitle())
                .examDate(req.getExamDate())
                .notes(req.getNotes())
                .build();

        log.info("Creating karate exam title={} batchId={}", req.getExamTitle(), batchId);
        return examRepo.save(exam);
    }

    @Override
    public List<KarateExam> listExams(Long batchId, AppUser user) {
        return examRepo.findByBatchIdOrderByExamDateAsc(batchId);
    }

    @Override
    @Transactional
    public KarateExamResult recordResult(Long examId, KarateExamResultRequest req, AppUser admin) {
        KarateExam exam = examRepo.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Karate exam not found: " + examId));
        AppUser student = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getUserId()));

        // Idempotent: update if result already exists
        KarateExamResult result = examResultRepo.findByExamIdAndUserId(examId, req.getUserId())
                .orElse(KarateExamResult.builder().exam(exam).user(student).build());

        result.setResult(req.getResult());
        result.setScore(req.getScore());
        result.setExaminerNotes(req.getExaminerNotes());

        boolean award = Boolean.TRUE.equals(req.getAwardBelt()) && "PASS".equals(req.getResult());
        result.setBeltAwarded(award);
        if (award) {
            result.setAwardedBelt(exam.getTargetBelt());
            // Update the enrollment's current belt
            enrollmentRepo.findByBatchIdAndUserId(exam.getBatch().getId(), student.getId())
                    .ifPresent(enrollment -> {
                        enrollment.setCurrentBelt(exam.getTargetBelt());
                        enrollmentRepo.save(enrollment);
                    });
        }

        log.info("Recording karate exam result userId={} examId={} result={}", req.getUserId(), examId, req.getResult());
        return examResultRepo.save(result);
    }

    @Override
    public List<KarateExamResult> listResults(Long examId, AppUser admin) {
        return examResultRepo.findByExamIdOrderByRecordedAtAsc(examId);
    }
}
