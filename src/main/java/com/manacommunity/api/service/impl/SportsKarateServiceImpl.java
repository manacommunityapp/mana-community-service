package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.karate.*;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsCourt;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.model.karate.*;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.SportsMetaRepository;
import com.manacommunity.api.repository.VenueRepository;
import com.manacommunity.api.repository.karate.*;
import com.manacommunity.api.service.SportsKarateService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsKarateServiceImpl implements SportsKarateService {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final int LOW_ATT_ALERT_COOLDOWN_DAYS = 7;

    private final SportsKarateBeltRepository beltRepo;
    private final SportsKarateProgramRepository programRepo;
    private final SportsKarateBatchRepository batchRepo;
    private final SportsKarateEnrollmentRepository enrollmentRepo;
    private final SportsKarateClassRepository classRepo;
    private final SportsKarateAttendanceRepository attendanceRepo;
    private final SportsKarateGradingExamRepository examRepo;
    private final SportsKarateExamResultRepository examResultRepo;

    private final CommunityRepository communityRepo;
    private final SportsMetaRepository sportsMetaRepo;
    private final AppUserRepository userRepo;
    private final VenueRepository venueRepo;

    // ── Helpers ────────────────────────────────────────────────────────────

    private Community resolveCommunity(AppUser caller, Long requestedId) {
        Long id = caller.hasRole(ROLE_SUPER_ADMIN) ? requestedId
                : (caller.getCommunity() != null ? caller.getCommunity().getId() : null);
        if (id == null) throw new InvalidInputException("Community could not be determined for caller.");
        return communityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community", id));
    }

    private Long callerCommunityId(AppUser caller) {
        return caller.getCommunity() != null ? caller.getCommunity().getId() : null;
    }

    // ── Belts ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateBelt> getBelts(AppUser caller) {
        Long cid = callerCommunityId(caller);
        return cid != null
                ? beltRepo.findByCommunityIdAndActiveTrueOrderByRankAsc(cid)
                : beltRepo.findAll();
    }

    @Override
    @Transactional
    public SportsKarateBelt createBelt(KarateBeltRequest req, AppUser caller) {
        Community community = resolveCommunity(caller, req.getCommunityId());
        if (beltRepo.existsByCommunityIdAndRank(community.getId(), req.getRank())) {
            throw new InvalidInputException("A belt at rank " + req.getRank() + " already exists for this community.");
        }
        SportsMeta sport = req.getSportId() != null
                ? sportsMetaRepo.findById(req.getSportId())
                    .orElseThrow(() -> new ResourceNotFoundException("SportsMeta", req.getSportId()))
                : null;
        SportsKarateBelt belt = SportsKarateBelt.builder()
                .community(community).sport(sport)
                .name(req.getName()).colorHex(req.getColorHex()).rank(req.getRank())
                .minClassesRequired(req.getMinClassesRequired())
                .minMonthsRequired(req.getMinMonthsRequired())
                .description(req.getDescription()).active(true)
                .build();
        return beltRepo.save(belt);
    }

    @Override
    @Transactional
    public SportsKarateBelt updateBelt(Long id, KarateBeltRequest req) {
        SportsKarateBelt belt = beltRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBelt", id));
        belt.setName(req.getName());
        if (req.getColorHex() != null) belt.setColorHex(req.getColorHex());
        if (req.getMinClassesRequired() != null) belt.setMinClassesRequired(req.getMinClassesRequired());
        if (req.getMinMonthsRequired() != null) belt.setMinMonthsRequired(req.getMinMonthsRequired());
        if (req.getDescription() != null) belt.setDescription(req.getDescription());
        return beltRepo.save(belt);
    }

    @Override
    @Transactional
    public void deleteBelt(Long id) {
        beltRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SportsKarateBelt", id));
        beltRepo.deleteById(id);
    }

    // ── Programs ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<SportsKarateProgram> getPrograms(AppUser caller, Pageable pageable) {
        Long cid = callerCommunityId(caller);
        return cid != null
                ? programRepo.findByCommunityId(cid, pageable)
                : programRepo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsKarateProgram getProgram(Long id) {
        return programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateProgram", id));
    }

    @Override
    @Transactional
    public SportsKarateProgram createProgram(KarateProgramRequest req, AppUser caller) {
        Community community = resolveCommunity(caller, req.getCommunityId());
        SportsMeta sport = sportsMetaRepo.findById(req.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsMeta", req.getSportId()));
        AppUser instructor = req.getInstructorUserId() != null
                ? userRepo.findById(req.getInstructorUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.getInstructorUserId()))
                : null;
        SportsKarateProgram program = SportsKarateProgram.builder()
                .community(community).sport(sport).instructor(instructor)
                .name(req.getName())
                .level(req.getLevel() != null ? req.getLevel() : SportsKarateProgram.ProgramLevel.BEGINNER)
                .minAge(req.getMinAge()).maxAge(req.getMaxAge())
                .monthlyFee(req.getMonthlyFee()).startDate(req.getStartDate()).endDate(req.getEndDate())
                .maxStudents(req.getMaxStudents())
                .active(req.getActive() != null ? req.getActive() : true)
                .description(req.getDescription())
                .build();
        return programRepo.save(program);
    }

    @Override
    @Transactional
    public SportsKarateProgram updateProgram(Long id, KarateProgramRequest req) {
        SportsKarateProgram p = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateProgram", id));
        if (req.getName() != null) p.setName(req.getName());
        if (req.getLevel() != null) p.setLevel(req.getLevel());
        if (req.getMinAge() != null) p.setMinAge(req.getMinAge());
        if (req.getMaxAge() != null) p.setMaxAge(req.getMaxAge());
        if (req.getMonthlyFee() != null) p.setMonthlyFee(req.getMonthlyFee());
        if (req.getStartDate() != null) p.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) p.setEndDate(req.getEndDate());
        if (req.getMaxStudents() != null) p.setMaxStudents(req.getMaxStudents());
        if (req.getActive() != null) p.setActive(req.getActive());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getInstructorUserId() != null) {
            p.setInstructor(userRepo.findById(req.getInstructorUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.getInstructorUserId())));
        }
        return programRepo.save(p);
    }

    @Override
    @Transactional
    public void deleteProgram(Long id) {
        SportsKarateProgram p = programRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateProgram", id));
        List<SportsKarateBatch> batches = batchRepo.findByProgramIdAndStatus(id, SportsKarateBatch.BatchStatus.ACTIVE);
        if (!batches.isEmpty()) {
            throw new InvalidInputException("Cannot delete program with active batches. Deactivate batches first.");
        }
        programRepo.delete(p);
    }

    // ── Batches ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateBatch> getBatches(Long programId) {
        return batchRepo.findByProgramIdOrderByBatchNameAsc(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsKarateBatch getBatch(Long id) {
        return batchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBatch", id));
    }

    @Override
    @Transactional
    public SportsKarateBatch createBatch(Long programId, KarateBatchRequest req, AppUser caller) {
        SportsKarateProgram program = programRepo.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateProgram", programId));
        Venue venue = req.getVenueId() != null
                ? venueRepo.findById(req.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId()))
                : null;
        SportsKarateBatch batch = SportsKarateBatch.builder()
                .program(program).community(program.getCommunity())
                .venue(venue).batchName(req.getBatchName())
                .daysOfWeek(req.getDaysOfWeek())
                .startTime(req.getStartTime()).endTime(req.getEndTime())
                .maxStudents(req.getMaxStudents())
                .attendanceThreshold(req.getAttendanceThreshold() != null ? req.getAttendanceThreshold() : 75)
                .autoGenerateClasses(req.getAutoGenerateClasses() != null ? req.getAutoGenerateClasses() : true)
                .status(SportsKarateBatch.BatchStatus.UPCOMING)
                .build();
        return batchRepo.save(batch);
    }

    @Override
    @Transactional
    public SportsKarateBatch updateBatch(Long id, KarateBatchRequest req) {
        SportsKarateBatch b = batchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBatch", id));
        if (req.getBatchName() != null) b.setBatchName(req.getBatchName());
        if (req.getDaysOfWeek() != null) b.setDaysOfWeek(req.getDaysOfWeek());
        if (req.getStartTime() != null) b.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) b.setEndTime(req.getEndTime());
        if (req.getMaxStudents() != null) b.setMaxStudents(req.getMaxStudents());
        if (req.getAttendanceThreshold() != null) b.setAttendanceThreshold(req.getAttendanceThreshold());
        if (req.getAutoGenerateClasses() != null) b.setAutoGenerateClasses(req.getAutoGenerateClasses());
        if (req.getVenueId() != null) {
            b.setVenue(venueRepo.findById(req.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId())));
        }
        return batchRepo.save(b);
    }

    @Override
    @Transactional
    public SportsKarateBatch updateBatchStatus(Long id, SportsKarateBatch.BatchStatus status, AppUser caller) {
        SportsKarateBatch b = batchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBatch", id));
        b.setStatus(status);
        SportsKarateBatch saved = batchRepo.save(b);

        if (status == SportsKarateBatch.BatchStatus.ACTIVE && Boolean.TRUE.equals(b.getAutoGenerateClasses())) {
            SportsKarateProgram prog = b.getProgram();
            if (prog.getStartDate() != null && prog.getEndDate() != null) {
                KarateClassGenerateRequest gen = new KarateClassGenerateRequest();
                gen.setFromDate(prog.getStartDate());
                gen.setToDate(prog.getEndDate());
                generateClasses(id, gen, caller);
            }
        }
        return saved;
    }

    // ── Enrollments ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SportsKarateEnrollment enroll(Long batchId, KarateEnrollRequest req, AppUser caller) {
        SportsKarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBatch", batchId));
        AppUser student = userRepo.findById(req.getStudentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", req.getStudentUserId()));

        if (enrollmentRepo.existsByBatchIdAndStudentId(batchId, student.getId())) {
            throw new InvalidInputException("Student is already enrolled in this batch.");
        }
        if (batch.getMaxStudents() != null) {
            long current = batchRepo.countActiveEnrollments(batchId);
            if (current >= batch.getMaxStudents()) {
                throw new InvalidInputException("Batch is full (max " + batch.getMaxStudents() + " students).");
            }
        }
        SportsKarateBelt belt = req.getInitialBeltId() != null
                ? beltRepo.findById(req.getInitialBeltId())
                    .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBelt", req.getInitialBeltId()))
                : null;

        SportsKarateEnrollment enrollment = SportsKarateEnrollment.builder()
                .batch(batch).community(batch.getCommunity()).student(student)
                .currentBelt(belt).enrolledBy(caller)
                .status(SportsKarateEnrollment.EnrollmentStatus.ACTIVE)
                .enrolledAt(LocalDateTime.now()).notes(req.getNotes())
                .build();
        return enrollmentRepo.save(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateEnrollment> getEnrollments(Long batchId) {
        return enrollmentRepo.findByBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateEnrollment> getMyEnrollments(AppUser caller) {
        return enrollmentRepo.findByStudentId(caller.getId());
    }

    @Override
    @Transactional
    public SportsKarateEnrollment updateEnrollmentStatus(Long id, SportsKarateEnrollment.EnrollmentStatus status) {
        SportsKarateEnrollment e = enrollmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateEnrollment", id));
        e.setStatus(status);
        return enrollmentRepo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsKarateEnrollment getEnrollmentProgress(Long id) {
        return enrollmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateEnrollment", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateEnrollment> getGradingEligible(Long batchId) {
        return enrollmentRepo.findByBatchIdAndStatusAndGradingEligibleTrue(
                batchId, SportsKarateEnrollment.EnrollmentStatus.ACTIVE);
    }

    // ── Classes ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<SportsKarateClass> generateClasses(Long batchId, KarateClassGenerateRequest req, AppUser caller) {
        SportsKarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBatch", batchId));

        Set<DayOfWeek> scheduledDays = parseDaysOfWeek(batch.getDaysOfWeek());
        if (scheduledDays.isEmpty()) {
            throw new InvalidInputException("Batch has no days_of_week configured. Update the batch first.");
        }

        List<SportsKarateClass> created = new ArrayList<>();
        LocalDate current = req.getFromDate();
        while (!current.isAfter(req.getToDate())) {
            if (scheduledDays.contains(current.getDayOfWeek())
                    && !classRepo.existsByBatchIdAndScheduledDate(batchId, current)) {
                SportsKarateClass cls = SportsKarateClass.builder()
                        .batch(batch).community(batch.getCommunity())
                        .scheduledDate(current)
                        .startTime(batch.getStartTime()).endTime(batch.getEndTime())
                        .status(SportsKarateClass.ClassStatus.SCHEDULED)
                        .build();
                created.add(classRepo.save(cls));
            }
            current = current.plusDays(1);
        }
        log.info("Generated {} class sessions for batch {} between {} and {}",
                created.size(), batchId, req.getFromDate(), req.getToDate());
        return created;
    }

    /** Parses "MON,WED,FRI" into DayOfWeek set. */
    private Set<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        Set<DayOfWeek> days = new LinkedHashSet<>();
        if (daysOfWeek == null || daysOfWeek.isBlank()) return days;
        Map<String, DayOfWeek> map = Map.of(
                "MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY,
                "WED", DayOfWeek.WEDNESDAY, "THU", DayOfWeek.THURSDAY,
                "FRI", DayOfWeek.FRIDAY, "SAT", DayOfWeek.SATURDAY,
                "SUN", DayOfWeek.SUNDAY);
        for (String token : daysOfWeek.toUpperCase().split(",")) {
            DayOfWeek d = map.get(token.trim());
            if (d != null) days.add(d);
        }
        return days;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateClass> getClasses(Long batchId) {
        return classRepo.findByBatchIdOrderByScheduledDateAsc(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsKarateClass getClass(Long id) {
        return classRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateClass", id));
    }

    @Override
    @Transactional
    public SportsKarateClass updateClass(Long id, String topic, String classNotes) {
        SportsKarateClass c = classRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateClass", id));
        if (topic != null) c.setTopic(topic);
        if (classNotes != null) c.setClassNotes(classNotes);
        return classRepo.save(c);
    }

    @Override
    @Transactional
    public SportsKarateClass cancelClass(Long id, String reason, AppUser caller) {
        SportsKarateClass c = classRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateClass", id));
        c.setStatus(SportsKarateClass.ClassStatus.CANCELLED);
        c.setCancelReason(reason);
        return classRepo.save(c);
    }

    @Override
    @Transactional
    public SportsKarateClass startClass(Long id, AppUser caller) {
        SportsKarateClass c = classRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateClass", id));
        c.setStatus(SportsKarateClass.ClassStatus.ONGOING);
        c.setConductedBy(caller);
        return classRepo.save(c);
    }

    @Override
    @Transactional
    public SportsKarateClass completeClass(Long id, AppUser caller) {
        SportsKarateClass c = classRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateClass", id));
        c.setStatus(SportsKarateClass.ClassStatus.COMPLETED);
        SportsKarateClass saved = classRepo.save(c);
        recomputeAttendanceForBatch(c.getBatch().getId());
        return saved;
    }

    // ── Attendance ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<SportsKarateAttendance> markAttendance(Long classId,
                                                       List<KarateAttendanceEntry> entries, AppUser caller) {
        SportsKarateClass cls = classRepo.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateClass", classId));
        List<SportsKarateAttendance> saved = new ArrayList<>();
        for (KarateAttendanceEntry entry : entries) {
            SportsKarateEnrollment enrollment = enrollmentRepo.findById(entry.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("SportsKarateEnrollment", entry.getEnrollmentId()));

            SportsKarateAttendance attendance = attendanceRepo
                    .findByClassSessionIdAndEnrollmentId(classId, enrollment.getId())
                    .orElseGet(() -> SportsKarateAttendance.builder()
                            .classSession(cls).enrollment(enrollment)
                            .community(cls.getCommunity())
                            .build());

            attendance.setStatus(entry.getStatus());
            attendance.setMarkedAt(LocalDateTime.now());
            attendance.setMarkedBy(caller);
            attendance.setNotes(entry.getNotes());
            saved.add(attendanceRepo.save(attendance));
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateAttendance> getAttendanceForClass(Long classId) {
        return attendanceRepo.findByClassSessionId(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateAttendance> getAttendanceForEnrollment(Long enrollmentId) {
        return attendanceRepo.findByEnrollmentIdOrdered(enrollmentId);
    }

    @Override
    @Transactional
    public SportsKarateAttendance updateAttendance(Long attendanceId,
                                                   SportsKarateAttendance.AttendanceStatus status, String notes) {
        SportsKarateAttendance a = attendanceRepo.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateAttendance", attendanceId));
        a.setStatus(status);
        if (notes != null) a.setNotes(notes);
        SportsKarateAttendance saved = attendanceRepo.save(a);
        recomputeAttendanceForBatch(a.getEnrollment().getBatch().getId());
        return saved;
    }

    /** Recomputes attendance percentage for all active enrollments in a batch. */
    public void recomputeAttendanceForBatch(Long batchId) {
        long totalCompleted = classRepo.countByBatchIdAndStatus(batchId, SportsKarateClass.ClassStatus.COMPLETED);
        if (totalCompleted == 0) return;

        List<SportsKarateEnrollment> enrollments = enrollmentRepo.findByBatchIdAndStatus(
                batchId, SportsKarateEnrollment.EnrollmentStatus.ACTIVE);

        for (SportsKarateEnrollment e : enrollments) {
            long attended = attendanceRepo.countAttendedByEnrollment(e.getId());
            BigDecimal pct = BigDecimal.valueOf(attended * 100.0 / totalCompleted)
                    .setScale(2, RoundingMode.HALF_UP);
            e.setTotalClassesAttended((int) attended);
            e.setAttendancePercentage(pct);

            SportsKarateBatch batch = e.getBatch();
            BigDecimal threshold = BigDecimal.valueOf(batch.getAttendanceThreshold());
            if (pct.compareTo(threshold) < 0) {
                checkAndSendLowAttAlert(e, pct, threshold);
            }
            enrollmentRepo.save(e);
        }
    }

    private void checkAndSendLowAttAlert(SportsKarateEnrollment e,
                                         BigDecimal pct, BigDecimal threshold) {
        LocalDateTime lastAlert = e.getLastLowAttAlertAt();
        if (lastAlert != null && lastAlert.plusDays(LOW_ATT_ALERT_COOLDOWN_DAYS).isAfter(LocalDateTime.now())) {
            return;
        }
        log.warn("Low attendance alert: enrollment {} (student {}) is at {}% — below threshold {}%",
                e.getId(), e.getStudent().getId(), pct, threshold);
        e.setLastLowAttAlertAt(LocalDateTime.now());
    }

    // ── Grading Exams ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public SportsKarateGradingExam scheduleExam(Long batchId, KarateGradingExamRequest req, AppUser caller) {
        SportsKarateBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBatch", batchId));
        SportsKarateBelt targetBelt = beltRepo.findById(req.getTargetBeltId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBelt", req.getTargetBeltId()));
        Venue venue = req.getVenueId() != null
                ? venueRepo.findById(req.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId()))
                : null;
        SportsKarateGradingExam exam = SportsKarateGradingExam.builder()
                .batch(batch).community(batch.getCommunity())
                .targetBelt(targetBelt).venue(venue)
                .scheduledDate(req.getScheduledDate())
                .examinerName(req.getExaminerName())
                .maxCandidates(req.getMaxCandidates())
                .notes(req.getNotes())
                .status(SportsKarateGradingExam.ExamStatus.SCHEDULED)
                .build();
        return examRepo.save(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateGradingExam> getExams(Long batchId) {
        return examRepo.findByBatchIdOrderByScheduledDateAsc(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateEnrollment> getEligibleForExam(Long examId) {
        SportsKarateGradingExam exam = examRepo.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateGradingExam", examId));
        return enrollmentRepo.findByBatchIdAndStatusAndGradingEligibleTrue(
                exam.getBatch().getId(), SportsKarateEnrollment.EnrollmentStatus.ACTIVE);
    }

    @Override
    @Transactional
    public List<SportsKarateExamResult> submitResults(Long examId,
                                                      List<KarateExamResultEntry> entries, AppUser caller) {
        SportsKarateGradingExam exam = examRepo.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateGradingExam", examId));

        List<SportsKarateExamResult> results = new ArrayList<>();
        for (KarateExamResultEntry entry : entries) {
            SportsKarateEnrollment enrollment = enrollmentRepo.findById(entry.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("SportsKarateEnrollment", entry.getEnrollmentId()));

            SportsKarateExamResult result = examResultRepo
                    .findByExamIdAndEnrollmentId(examId, enrollment.getId())
                    .orElseGet(() -> SportsKarateExamResult.builder()
                            .exam(exam).enrollment(enrollment)
                            .community(exam.getCommunity())
                            .build());

            SportsKarateBelt newBelt = entry.getNewBeltId() != null
                    ? beltRepo.findById(entry.getNewBeltId())
                        .orElseThrow(() -> new ResourceNotFoundException("SportsKarateBelt", entry.getNewBeltId()))
                    : null;

            result.setPassed(entry.getPassed());
            result.setNewBelt(newBelt);
            result.setScore(entry.getScore());
            result.setRemarks(entry.getRemarks());
            result.setGradedAt(LocalDateTime.now());
            result.setGradedBy(caller);
            SportsKarateExamResult saved = examResultRepo.save(result);

            // Belt promotion — idempotent via beltUpdated flag
            if (Boolean.TRUE.equals(entry.getPassed()) && newBelt != null && !Boolean.TRUE.equals(result.getBeltUpdated())) {
                enrollment.setCurrentBelt(newBelt);
                enrollment.setGradingEligible(false);
                enrollmentRepo.save(enrollment);
                saved.setBeltUpdated(true);
                examResultRepo.save(saved);
                log.info("Belt promotion: enrollment {} promoted to {} ({})",
                        enrollment.getId(), newBelt.getName(), newBelt.getRank());
            }
            results.add(saved);
        }

        exam.setStatus(SportsKarateGradingExam.ExamStatus.COMPLETED);
        examRepo.save(exam);
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsKarateExamResult> getExamResults(Long examId) {
        return examResultRepo.findByExamId(examId);
    }

    @Override
    @Transactional
    public SportsKarateGradingExam cancelExam(Long id, AppUser caller) {
        SportsKarateGradingExam exam = examRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsKarateGradingExam", id));
        exam.setStatus(SportsKarateGradingExam.ExamStatus.CANCELLED);
        return examRepo.save(exam);
    }
}
